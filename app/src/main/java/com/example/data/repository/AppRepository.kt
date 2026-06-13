package com.example.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.util.Log
import com.example.BuildConfig
import com.example.data.database.AppDao
import com.example.data.database.AppDatabase
import com.example.data.model.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Source
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class AppRepository(private val context: Context) {
    private val appDatabase = AppDatabase.getDatabase(context)
    private val dao: AppDao = appDatabase.appDao()

    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // --- OkHttpClient for Gemini API ---
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    // --- Firestore Active Snapshot Listener Registrations ---
    private val listenerRegistrations = mutableListOf<ListenerRegistration>()

    // --- Real-time Local Memory Flows (Bypassing Room entirely for Services) ---
    private val _categories = MutableStateFlow<List<CategoryEntity>>(emptyList())
    val categories: Flow<List<CategoryEntity>> = _categories

    private val _allProviders = MutableStateFlow<List<ProviderEntity>>(emptyList())
    val allProviders: Flow<List<ProviderEntity>> = _allProviders
    
    val activeProviders: Flow<List<ProviderEntity>> = _allProviders.map { list ->
        list.filter { !it.isBlocked }
    }
    
    val recommendedProviders: Flow<List<ProviderEntity>> = _allProviders.map { list ->
        list.filter { it.isRecommended && !it.isBlocked }
    }

    private val _pendingProviders = MutableStateFlow<List<PendingProviderEntity>>(emptyList())
    val pendingProviders: Flow<List<PendingProviderEntity>> = _pendingProviders

    private val _pendingCount = MutableStateFlow<Int>(0)
    val pendingCount: Flow<Int> = _pendingCount

    private val _appSettings = MutableStateFlow<AppSettingEntity?>(null)
    val appSettings: Flow<AppSettingEntity?> = _appSettings

    private val _allBanners = MutableStateFlow<List<BannerEntity>>(emptyList())
    val allBanners: Flow<List<BannerEntity>> = _allBanners
    
    val activeBanners: Flow<List<BannerEntity>> = _allBanners.map { list ->
        list.filter { it.isActive }
    }

    private val _reports = MutableStateFlow<List<ReportEntity>>(emptyList())
    val reports: Flow<List<ReportEntity>> = _reports

    private val _reviews = MutableStateFlow<List<ReviewEntity>>(emptyList())
    val reviews: Flow<List<ReviewEntity>> = _reviews

    private val _allTerms = MutableStateFlow<List<RegistrationTermEntity>>(emptyList())
    val allTerms: Flow<List<RegistrationTermEntity>> = _allTerms
    
    val activeTerms: Flow<List<RegistrationTermEntity>> = _allTerms.map { list ->
        list.filter { it.isActive }.sortedBy { it.order }
    }

    // Keep Chats & Local logs inside Room database for offline speed/messages insulation
    val chats: Flow<List<ChatEntity>> = dao.getAllChatsFlow()
    val activityLogs: Flow<List<ActivityLogEntity>> = dao.getAllActivityLogsFlow()

    fun getMessagesForChat(chatId: String): Flow<List<MessageEntity>> = dao.getMessagesForChatFlow(chatId)
    
    fun getReviewsForProvider(providerId: String): Flow<List<ReviewEntity>> = _reviews.map { list ->
        list.filter { it.providerId == providerId }
    }

    // --- Network Reconnection Listener & Immediate Cache Loading initialization ---
    private var wasConnectedBeforeNetworkChange = false

    init {
        wasConnectedBeforeNetworkChange = isConnected()
        
        // Setup connectivity manager callback to capture transition to online
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        try {
            connectivityManager.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    if (!wasConnectedBeforeNetworkChange) {
                        wasConnectedBeforeNetworkChange = true
                        Log.d("AppRepository", "Connection recovered! Re-subscribing snapshot listeners...")
                        repositoryScope.launch {
                            forceReSubscribe()
                        }
                    }
                }
                override fun onLost(network: Network) {
                    wasConnectedBeforeNetworkChange = false
                    Log.d("AppRepository", "Connection lost")
                }
            })
        } catch (e: Exception) {
            Log.e("AppRepository", "Error registering network status callback", e)
        }

        // Load Cache immediately for instant rendering, then boot listeners
        repositoryScope.launch {
            loadInitialFromCache()
            startAllSnapshotListeners()
        }
    }

    // --- Core helper to await play-services Tasks inside coroutines ---
    private suspend fun <T> com.google.android.gms.tasks.Task<T>.awaitTask(): T = suspendCancellableCoroutine { continuation ->
        addOnCompleteListener { task ->
            if (task.isSuccessful) {
                continuation.resume(task.result)
            } else {
                continuation.resumeWithException(task.exception ?: Exception("Firestore task failed"))
            }
        }
    }

    // Fast cache loader to satisfy online-offline seamless boots
    private suspend fun loadInitialFromCache() {
        try {
            val db = FirebaseFirestore.getInstance()
            
            // 1. Categories
            val catSnap = db.collection("categories").get(Source.CACHE).awaitTask()
            val catObjects = catSnap.toObjects(CategoryEntity::class.java)
            if (catObjects.isNotEmpty()) _categories.value = catObjects.sortedBy { it.order }

            // 2. Providers
            val provSnap = db.collection("service_providers").get(Source.CACHE).awaitTask()
            val provObjects = provSnap.toObjects(ProviderEntity::class.java)
            if (provObjects.isNotEmpty()) _allProviders.value = provObjects

            // 3. Pending registrations
            val pendSnap = db.collection("pending_providers").get(Source.CACHE).awaitTask()
            val pendObjects = pendSnap.toObjects(PendingProviderEntity::class.java)
            if (pendObjects.isNotEmpty()) {
                _pendingProviders.value = pendObjects
                _pendingCount.value = pendObjects.filter { it.status == "pending" }.size
            }

            // 4. App settings
            val setSnap = db.collection("app_settings").document("master").get(Source.CACHE).awaitTask()
            val appParam = setSnap.toObject(AppSettingEntity::class.java)
            if (appParam != null) _appSettings.value = appParam

            // 5. Banners
            val bSnap = db.collection("banners").get(Source.CACHE).awaitTask()
            val bObjects = bSnap.toObjects(BannerEntity::class.java)
            if (bObjects.isNotEmpty()) _allBanners.value = bObjects

            // 6. Reports
            val rSnap = db.collection("reports").get(Source.CACHE).awaitTask()
            val rObjects = rSnap.toObjects(ReportEntity::class.java)
            if (rObjects.isNotEmpty()) _reports.value = rObjects

            // 7. Reviews
            val revSnap = db.collection("reviews").get(Source.CACHE).awaitTask()
            val revObjects = revSnap.toObjects(ReviewEntity::class.java)
            if (revObjects.isNotEmpty()) _reviews.value = revObjects

            // 8. Terms of Registration
            val tSnap = db.collection("registration_terms").get(Source.CACHE).awaitTask()
            val tObjects = tSnap.toObjects(RegistrationTermEntity::class.java)
            if (tObjects.isNotEmpty()) _allTerms.value = tObjects
            
        } catch (e: Exception) {
            Log.d("AppRepository", "Initial Firestore local CACHE not ready yet / empty (expected on first runs)")
        }
    }

    @Synchronized
    private fun startAllSnapshotListeners() {
        // Clear old registrations before attaching new ones
        clearAllSnapshotListeners()
        
        val db = FirebaseFirestore.getInstance()

        // Sync and Seed if collections are empty inside blank Firebase project
        seedFirestoreIfNeeded()

        // 1. Categories
        val catsReg = db.collection("categories")
            .addSnapshotListener { snapshot, error ->
                if (error != null) { Log.e("AppRepository", "Categories Snapshot Error", error); return@addSnapshotListener }
                if (snapshot != null) {
                    val list = snapshot.toObjects(CategoryEntity::class.java)
                    _categories.value = list.sortedBy { it.order }
                }
            }
        listenerRegistrations.add(catsReg)

        // 2. Providers
        val provsReg = db.collection("service_providers")
            .addSnapshotListener { snapshot, error ->
                if (error != null) { Log.e("AppRepository", "Providers Snapshot Error", error); return@addSnapshotListener }
                if (snapshot != null) {
                    val list = snapshot.toObjects(ProviderEntity::class.java)
                    _allProviders.value = list
                }
            }
        listenerRegistrations.add(provsReg)

        // 3. Pending
        val pendReg = db.collection("pending_providers")
            .addSnapshotListener { snapshot, error ->
                if (error != null) { Log.e("AppRepository", "Pending Snapshot Error", error); return@addSnapshotListener }
                if (snapshot != null) {
                    val list = snapshot.toObjects(PendingProviderEntity::class.java)
                    _pendingProviders.value = list
                    _pendingCount.value = list.filter { it.status == "pending" }.size
                }
            }
        listenerRegistrations.add(pendReg)

        // 4. AppSettings
        val setReg = db.collection("app_settings").document("master")
            .addSnapshotListener { snapshot, error ->
                if (error != null) { Log.e("AppRepository", "Settings Snapshot Error", error); return@addSnapshotListener }
                if (snapshot != null) {
                    val settings = snapshot.toObject(AppSettingEntity::class.java)
                    if (settings != null) _appSettings.value = settings
                }
            }
        listenerRegistrations.add(setReg)

        // 5. Banners
        val bannersReg = db.collection("banners")
            .addSnapshotListener { snapshot, error ->
                if (error != null) { Log.e("AppRepository", "Banners Snapshot Error", error); return@addSnapshotListener }
                if (snapshot != null) {
                    val list = snapshot.toObjects(BannerEntity::class.java)
                    _allBanners.value = list
                }
            }
        listenerRegistrations.add(bannersReg)

        // 6. Reports
        val reportsReg = db.collection("reports")
            .addSnapshotListener { snapshot, error ->
                if (error != null) { Log.e("AppRepository", "Reports Snapshot Error", error); return@addSnapshotListener }
                if (snapshot != null) {
                    val list = snapshot.toObjects(ReportEntity::class.java)
                    _reports.value = list
                }
            }
        listenerRegistrations.add(reportsReg)

        // 7. Reviews
        val reviewsReg = db.collection("reviews")
            .addSnapshotListener { snapshot, error ->
                if (error != null) { Log.e("AppRepository", "Reviews Snapshot Error", error); return@addSnapshotListener }
                if (snapshot != null) {
                    val list = snapshot.toObjects(ReviewEntity::class.java)
                    _reviews.value = list
                }
            }
        listenerRegistrations.add(reviewsReg)

        // 8. Terms
        val termsReg = db.collection("registration_terms")
            .addSnapshotListener { snapshot, error ->
                if (error != null) { Log.e("AppRepository", "Terms Snapshot Error", error); return@addSnapshotListener }
                if (snapshot != null) {
                    val list = snapshot.toObjects(RegistrationTermEntity::class.java)
                    _allTerms.value = list
                }
            }
        listenerRegistrations.add(termsReg)
        
        Log.d("AppRepository", "Real-time Cloud Firestore snapshot listeners initiated.")
    }

    @Synchronized
    private fun clearAllSnapshotListeners() {
        listenerRegistrations.forEach { it.remove() }
        listenerRegistrations.clear()
        Log.d("AppRepository", "All snapshot listeners cleared.")
    }

    private fun forceReSubscribe() {
        clearAllSnapshotListeners()
        startAllSnapshotListeners()
    }

    // Cloud side seeding to ensure completely instant onboarding if Firestore collection is blank
    private fun seedFirestoreIfNeeded() {
        val db = FirebaseFirestore.getInstance()
        db.collection("app_settings").document("master").get().addOnSuccessListener { doc ->
            if (!doc.exists()) {
                Log.d("AppRepository", "No app configuration found in Firestore. Seeding defaults...")
                val initialSettings = AppSettingEntity(
                    id = "master",
                    appName = "دليل خدمات اليمن",
                    welcomeMessage = "أهلاً ومرحباً بكم مع تطبيق كل خدمات اليمن - دليل أصحاب المهن الفاخر",
                    footerText = "دليلي ومساعدك الذكي WAM 777644670",
                    supportPhone = "777644670",
                    supportEmail = "support@dalyly2026.com",
                    supportWhatsApp = "+967777644670",
                    primaryColor = "#ECEFF1",
                    secondaryColor = "#37474F",
                    fontFamily = "Tajawal",
                    fontSize = 14,
                    chatEnabled = true,
                    assistantEnabled = true,
                    radiusSearchLimit = 50,
                    voiceSearchEnabled = true,
                    maintenanceMode = false,
                    maintenanceMessage = ""
                )
                db.collection("app_settings").document("master").set(initialSettings)

                val listCats = listOf(
                    CategoryEntity("c1", "السباكة ومواسير المياه", "Plumbing Services", "https://cdn-icons-png.flaticon.com/512/3095/3095147.png", null, 1, true),
                    CategoryEntity("c2", "الكهرباء والطاقة الشمسية", "Electrical & Solar", "https://cdn-icons-png.flaticon.com/512/2984/2984024.png", null, 2, true),
                    CategoryEntity("c3", "الدهان والديكورات الحديثة", "Painting & Decor", "https://cdn-icons-png.flaticon.com/512/2970/2970922.png", null, 3, true),
                    CategoryEntity("c4", "النجارة وتصليح الأثاث", "Carpentry & Furniture", "https://cdn-icons-png.flaticon.com/512/3257/3257385.png", null, 4, true),
                    CategoryEntity("c5", "الحدادة والإنشاءات الحديدية", "Ironmongery & Blacksmith", "https://cdn-icons-png.flaticon.com/512/2921/2921226.png", null, 5, true)
                )
                listCats.forEach { db.collection("categories").document(it.id).set(it) }

                val listTerms = listOf(
                    RegistrationTermEntity("term1", "الالتزام بالصدق والأمانة في التعامل وتحديد الأسعار المناسبة.", 1, true),
                    RegistrationTermEntity("term2", "أن يكون المهني حاصلاً على الخبرة الميدانية المؤكدة في تخصص خدمته.", 2, true),
                    RegistrationTermEntity("term3", "صورة بطاقة الهوية إجبارية لأجل توثيق الشارة الزرقاء وتفعيل الضمان للمستخدم.", 3, true)
                )
                listTerms.forEach { db.collection("registration_terms").document(it.id).set(it) }

                val banner = BannerEntity(
                    id = "banner1",
                    title = "العرض الافتتاحي: سجل مجاناً هذا الشهر لتوثيق علامتك وشارتك في دليل اليمن!",
                    type = "text",
                    mediaUrl = "",
                    redirectLink = "register",
                    size = "M",
                    durationSeconds = 6,
                    isActive = true,
                    createdAt = System.currentTimeMillis()
                )
                db.collection("banners").document(banner.id).set(banner)

                val p1 = ProviderEntity(
                    id = "prov_sample1",
                    fullName = "ماهر محمد طاهر",
                    phone = "777644670",
                    mainCategoryId = "c1",
                    subCategoryId = "",
                    address = "صنعاء - شارع حدة",
                    district = "منطقة السبعين",
                    locationLat = 15.3186,
                    locationLng = 44.2045,
                    profileImageUrl = "",
                    idCardImageUrl = "",
                    isPinned = true,
                    isRecommended = true,
                    isVerified = true,
                    isBlocked = false,
                    averageRating = 5.0,
                    totalReviews = 0,
                    isSubscribed = true,
                    subscriptionExpiry = System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000),
                    fcmToken = "",
                    createdAt = System.currentTimeMillis()
                )
                val p2 = ProviderEntity(
                    id = "prov_sample2",
                    fullName = "علي أحمد الكبسي",
                    phone = "733829103",
                    mainCategoryId = "c2",
                    subCategoryId = "",
                    address = "صنعاء - الدائري الغربي",
                    district = "معين",
                    locationLat = 15.3456,
                    locationLng = 44.1812,
                    profileImageUrl = "",
                    idCardImageUrl = "",
                    isPinned = false,
                    isRecommended = false,
                    isVerified = true,
                    isBlocked = false,
                    averageRating = 4.5,
                    totalReviews = 0,
                    isSubscribed = false,
                    subscriptionExpiry = 0L,
                    fcmToken = "",
                    createdAt = System.currentTimeMillis()
                )
                db.collection("service_providers").document(p1.id).set(p1)
                db.collection("service_providers").document(p2.id).set(p2)
            }
        }
    }

    // --- Dynamic Settings and Admins Actions ---
    suspend fun saveSettings(settings: AppSettingEntity) = withContext(Dispatchers.IO) {
        FirebaseFirestore.getInstance().collection("app_settings").document("master")
            .set(settings).awaitTask()
        logAction("SYSTEM", "تحديث الإعدادات", "تم تعديل خيارات وألوان ولغة التطبيق بنجاح")
    }

    suspend fun logAction(adminId: String, action: String, details: String) = withContext(Dispatchers.IO) {
        dao.insertActivityLog(
            ActivityLogEntity(
                id = UUID.randomUUID().toString(),
                adminId = adminId,
                action = action,
                details = details,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    // --- Categories management ---
    suspend fun insertCategory(category: CategoryEntity) = withContext(Dispatchers.IO) {
        FirebaseFirestore.getInstance().collection("categories").document(category.id)
            .set(category).awaitTask()
        logAction("ADMIN", "إضافة قسم", "تم إضافة أو تعديل القسم: ${category.nameAr}")
    }

    suspend fun deleteCategory(id: String) = withContext(Dispatchers.IO) {
        FirebaseFirestore.getInstance().collection("categories").document(id)
            .delete().awaitTask()
        logAction("ADMIN", "حذف قسم", "تم حذف معرف القسم $id")
    }

    // --- Professionals Registrations (Firestore-based) ---
    suspend fun submitProviderRegistration(
        fullName: String,
        phone: String,
        mainCatId: String,
        subCatId: String,
        address: String,
        district: String,
        lat: Double,
        lng: Double,
        profileImageUri: String,
        idCardImageUri: String,
        workImagesCSV: String = ""
    ) = withContext(Dispatchers.IO) {
        val pendingId = "pend_${UUID.randomUUID()}"
        val newPending = PendingProviderEntity(
            id = pendingId,
            fullName = fullName,
            phone = phone,
            mainCategoryId = mainCatId,
            subCategoryId = subCatId,
            address = address,
            district = district,
            locationLat = lat,
            locationLng = lng,
            profileImageUrl = profileImageUri,
            idCardImageUrl = idCardImageUri,
            status = "pending",
            rejectReason = "",
            createdAt = System.currentTimeMillis(),
            workImagesCSV = workImagesCSV
        )
        FirebaseFirestore.getInstance().collection("pending_providers").document(pendingId)
            .set(newPending).awaitTask()
        logAction("USER", "تسجيل مهني جديد", "قدم المهني $fullName طلباً للتطبيق")
    }

    // --- Admin decision actions ---
    suspend fun acceptProviderRegistration(pendingId: String) = withContext(Dispatchers.IO) {
        val db = FirebaseFirestore.getInstance()
        val doc = db.collection("pending_providers").document(pendingId).get().awaitTask()
        val pending = doc.toObject(PendingProviderEntity::class.java)
        if (pending != null) {
            val provider = ProviderEntity(
                id = pending.id.replace("pend_", "prov_"),
                fullName = pending.fullName,
                phone = pending.phone,
                mainCategoryId = pending.mainCategoryId,
                subCategoryId = pending.subCategoryId,
                address = pending.address,
                district = pending.district,
                locationLat = pending.locationLat,
                locationLng = pending.locationLng,
                profileImageUrl = pending.profileImageUrl,
                idCardImageUrl = pending.idCardImageUrl,
                isPinned = false,
                isRecommended = false,
                isVerified = false,
                isBlocked = false,
                averageRating = 5.0,
                totalReviews = 0,
                isSubscribed = false,
                subscriptionExpiry = 0L,
                fcmToken = "",
                createdAt = System.currentTimeMillis(),
                workImagesCSV = pending.workImagesCSV
            )
            db.collection("service_providers").document(provider.id).set(provider).awaitTask()
            db.collection("pending_providers").document(pendingId).delete().awaitTask()
            logAction("ADMIN", "قبول طلب", "تم بنجاح قبول وتفعيل الحساب للمهني: ${provider.fullName}")
        }
    }

    suspend fun updateProviderDetails(
        id: String,
        fullName: String,
        phone: String,
        address: String,
        district: String,
        profileImageUrl: String,
        workImagesCSV: String
    ) = withContext(Dispatchers.IO) {
        val db = FirebaseFirestore.getInstance()
        val doc = db.collection("service_providers").document(id).get().awaitTask()
        val target = doc.toObject(ProviderEntity::class.java)
        if (target != null) {
            val updated = target.copy(
                fullName = fullName,
                phone = phone,
                address = address,
                district = district,
                profileImageUrl = profileImageUrl,
                workImagesCSV = workImagesCSV
            )
            db.collection("service_providers").document(id).set(updated).awaitTask()
            logAction("ADMIN", "تعديل تفاصيل مهني", "تم تعديل معلومات مقدم الخدمة $fullName من قبل الإدارة")
        }
    }

    suspend fun rejectProviderRegistration(pendingId: String, reason: String) = withContext(Dispatchers.IO) {
        val db = FirebaseFirestore.getInstance()
        val doc = db.collection("pending_providers").document(pendingId).get().awaitTask()
        val pending = doc.toObject(PendingProviderEntity::class.java)
        if (pending != null) {
            val rejected = pending.copy(status = "rejected", rejectReason = reason)
            db.collection("pending_providers").document(pendingId).set(rejected).awaitTask()
            logAction("ADMIN", "رفض طلب", "تم رفض طلب المهني ${pending.fullName} بسبب: $reason")
        }
    }

    suspend fun addProviderDirectly(
        fullName: String,
        phone: String,
        mainCatId: String,
        subCatId: String,
        address: String,
        district: String,
        isVip: Boolean
    ) = withContext(Dispatchers.IO) {
        val provider = ProviderEntity(
            id = "prov_${UUID.randomUUID()}",
            fullName = fullName,
            phone = phone,
            mainCategoryId = mainCatId,
            subCategoryId = subCatId,
            address = address,
            district = district,
            locationLat = 15.3186,
            locationLng = 44.2045,
            profileImageUrl = "",
            idCardImageUrl = "",
            isPinned = isVip,
            isRecommended = isVip,
            isVerified = true,
            isBlocked = false,
            averageRating = 5.0,
            totalReviews = 0,
            isSubscribed = isVip,
            subscriptionExpiry = if (isVip) System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000) else 0L,
            fcmToken = "",
            createdAt = System.currentTimeMillis()
        )
        FirebaseFirestore.getInstance().collection("service_providers").document(provider.id)
            .set(provider).awaitTask()
        logAction("ADMIN", "إضافة فني يدوياً", "تم إضافة الفني ${provider.fullName} مباشرة للدليل")
    }

    suspend fun deleteProvider(id: String) = withContext(Dispatchers.IO) {
        FirebaseFirestore.getInstance().collection("service_providers").document(id)
            .delete().awaitTask()
        logAction("ADMIN", "حذف فني نشط", "تمت إزالة المهني ذو المعرف $id من الدليل")
    }

    suspend fun updateProviderFeatures(
        id: String,
        isVerified: Boolean,
        isPinned: Boolean,
        isRecommended: Boolean,
        isSubscribed: Boolean
    ) = withContext(Dispatchers.IO) {
        val db = FirebaseFirestore.getInstance()
        val doc = db.collection("service_providers").document(id).get().awaitTask()
        val target = doc.toObject(ProviderEntity::class.java)
        if (target != null) {
            val updated = target.copy(
                isVerified = isVerified,
                isPinned = isPinned,
                isRecommended = isRecommended,
                isSubscribed = isSubscribed,
                subscriptionExpiry = if (isSubscribed) System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000) else 0L
            )
            db.collection("service_providers").document(id).set(updated).awaitTask()
            logAction("ADMIN", "تعديل مزايا مهني", "تم تعديل المزايا والموثوقية للمهني ${target.fullName}")
        }
    }

    // --- Reviews and loyalty points (Real-time recalculation across firestore) ---
    suspend fun submitReview(providerId: String, userId: String, rating: Int, comment: String) = withContext(Dispatchers.IO) {
        val db = FirebaseFirestore.getInstance()
        val reviewId = "rev_${UUID.randomUUID()}"
        val newReview = ReviewEntity(reviewId, providerId, userId, rating, comment, System.currentTimeMillis())
        db.collection("reviews").document(reviewId).set(newReview).awaitTask()

        // Award 15 loyalty points to user locally
        val currentPoints = dao.getLoyaltyPointsForUser(userId)
        val pointsToSave = (currentPoints?.points ?: 0) + 15
        dao.insertLoyaltyPoints(LoyaltyPointsEntity(userId, pointsToSave))

        // Re-calculate provider average rating from reviews collection
        val providerDoc = db.collection("service_providers").document(providerId).get().awaitTask()
        val provider = providerDoc.toObject(ProviderEntity::class.java)
        if (provider != null) {
            val reviewsSnapshot = db.collection("reviews").whereEqualTo("providerId", providerId).get().awaitTask()
            val rateFlow = reviewsSnapshot.toObjects(ReviewEntity::class.java)
            val totalQty = rateFlow.size
            val sumRates = rateFlow.sumOf { it.rating }
            val newAvg = if (totalQty > 0) sumRates.toDouble() / totalQty else rating.toDouble()
            db.collection("service_providers").document(providerId)
                .set(provider.copy(averageRating = newAvg, totalReviews = totalQty)).awaitTask()
        }
        logAction("USER", "تقييم مهني", "أعطى المستخدم $userId تقييماً بمقدار $rating للمهني $providerId")
    }

    suspend fun deleteReview(reviewId: String) = withContext(Dispatchers.IO) {
        FirebaseFirestore.getInstance().collection("reviews").document(reviewId)
            .delete().awaitTask()
        logAction("ADMIN", "مسح تقييم", "حذف التحكم تقييماً برقم $reviewId")
    }

    // --- Chats & Messages using Room ---
    suspend fun sendMessage(chatId: String, senderId: String, receiverId: String, text: String) = withContext(Dispatchers.IO) {
        val msgId = "msg_${UUID.randomUUID()}"
        val message = MessageEntity(
            messageId = msgId,
            chatId = chatId,
            senderId = senderId,
            receiverId = receiverId,
            message = text,
            timestamp = System.currentTimeMillis(),
            isRead = false
        )
        dao.insertMessage(message)

        val chat = ChatEntity(
            chatId = chatId,
            participantsCSV = "$senderId,$receiverId",
            lastMessage = text,
            lastUpdated = System.currentTimeMillis()
        )
        dao.insertChat(chat)
    }

    suspend fun clearAllChatHistory() = withContext(Dispatchers.IO) {
        dao.deleteAllChats()
        dao.deleteAllMessages()
        logAction("ADMIN", "مسح كل الدردشات", "تم تفريغ وحذف سجلات الدردشات بالكامل بطلب الأدمن")
    }

    // --- Banners on Firestore ---
    suspend fun addBanner(title: String, type: String, redirect: String, size: String, duration: Int) = withContext(Dispatchers.IO) {
        val b = BannerEntity(
            id = "ban_${UUID.randomUUID()}",
            title = title,
            type = type,
            mediaUrl = "",
            redirectLink = redirect,
            size = size,
            durationSeconds = duration,
            isActive = true,
            createdAt = System.currentTimeMillis()
        )
        FirebaseFirestore.getInstance().collection("banners").document(b.id).set(b).awaitTask()
        logAction("ADMIN", "زيادة ممول", "تم إضافة إعلان ممول باسم: $title")
    }

    suspend fun deleteBanner(id: String) = withContext(Dispatchers.IO) {
        FirebaseFirestore.getInstance().collection("banners").document(id).delete().awaitTask()
        logAction("ADMIN", "حذف إعلان", "تم مسح البانر الممول ذو الرقم $id")
    }

    // --- Registration Terms on Firestore ---
    suspend fun addRegistrationTerm(text: String, order: Int) = withContext(Dispatchers.IO) {
        val t = RegistrationTermEntity(
            id = "term_${UUID.randomUUID()}",
            termText = text,
            order = order,
            isActive = true
        )
        FirebaseFirestore.getInstance().collection("registration_terms").document(t.id).set(t).awaitTask()
    }

    suspend fun deleteRegistrationTerm(id: String) = withContext(Dispatchers.IO) {
        FirebaseFirestore.getInstance().collection("registration_terms").document(id).delete().awaitTask()
    }

    // --- Reports on Firestore ---
    suspend fun submitReport(providerId: String, userId: String, reason: String, details: String) = withContext(Dispatchers.IO) {
        val r = ReportEntity(
            id = "rep_${UUID.randomUUID()}",
            providerId = providerId,
            userId = userId,
            reason = reason,
            details = details,
            status = "pending",
            createdAt = System.currentTimeMillis()
        )
        FirebaseFirestore.getInstance().collection("reports").document(r.id).set(r).awaitTask()
        logAction("USER", "إرسال بلاغ عن فني", "تم الإبلاغ عن الفني $providerId بسبب $reason")
    }

    suspend fun deleteReport(id: String) = withContext(Dispatchers.IO) {
        FirebaseFirestore.getInstance().collection("reports").document(id).delete().awaitTask()
    }

    // --- Offline-first simulated database exports / Backups for Firestore variables ---
    suspend fun exportDatabaseToCSV(tableName: String): String = withContext(Dispatchers.IO) {
        val sb = java.lang.StringBuilder()
        when (tableName) {
            "providers" -> {
                sb.append("id,fullName,phone,mainCategory,address,rating,isSubscribed\n")
                val list = _allProviders.value
                list.forEach {
                    sb.append("${it.id},${it.fullName},${it.phone},${it.mainCategoryId},${it.address},${it.averageRating},${it.isSubscribed}\n")
                }
            }
            "chats" -> {
                sb.append("chatId,lastMessage,lastUpdated\n")
                val list = chats.firstOrNull() ?: emptyList()
                list.forEach {
                    sb.append("${it.chatId},${it.lastMessage.replace(",", " ")},${it.lastUpdated}\n")
                }
            }
            "logs" -> {
                sb.append("id,adminId,action,details,timestamp\n")
                val list = activityLogs.firstOrNull() ?: emptyList()
                list.forEach {
                    sb.append("${it.id},${it.adminId},${it.action},${it.details.replace(",", " ")},${it.timestamp}\n")
                }
            }
        }
        sb.toString()
    }

    suspend fun backupDatabaseToFile(destFolder: String): File = withContext(Dispatchers.IO) {
        val folderFile = File(destFolder)
        if (!folderFile.exists()) {
            folderFile.mkdirs()
        }
        val backupFile = File(folderFile, "wam_services_backup_${System.currentTimeMillis()}.json")
        val appSettingsObj = _appSettings.value
        val providersArray = JSONArray()
        _allProviders.value.forEach {
            val obj = JSONObject()
            obj.put("id", it.id)
            obj.put("fullName", it.fullName)
            obj.put("phone", it.phone)
            obj.put("mainCategoryId", it.mainCategoryId)
            obj.put("address", it.address)
            providersArray.put(obj)
        }

        val masterObj = JSONObject()
        masterObj.put("appName", appSettingsObj?.appName ?: "دليل خدمات اليمن")
        masterObj.put("footerText", appSettingsObj?.footerText ?: "MAW 777644670")
        masterObj.put("providers", providersArray)

        backupFile.writeText(masterObj.toString(4))
        logAction("ADMIN", "نسخ احتياطي", "تم تصدير نسخة احتياطية بنجاح إلى ملف")
        backupFile
    }

    suspend fun restoreDatabaseFromFile(file: File): Boolean = withContext(Dispatchers.IO) {
        try {
            val content = file.readText()
            val obj = JSONObject(content)
            val appName = obj.optString("appName", "دليل خدمات اليمن")
            val footerText = obj.optString("footerText", "MAW 777644670")
            
            val currentSettings = _appSettings.value ?: AppSettingEntity(
                id = "master",
                appName = appName,
                welcomeMessage = "أهلاً ومرحباً بكم مع تطبيق كل خدمات اليمن - دليل أصحاب المهن الفاخر",
                footerText = footerText,
                supportPhone = "777644670",
                supportEmail = "support@wam2026.com",
                supportWhatsApp = "+967777644670",
                primaryColor = "#ECEFF1",
                secondaryColor = "#37474F",
                fontFamily = "Tajawal",
                fontSize = 14,
                chatEnabled = true,
                assistantEnabled = true,
                radiusSearchLimit = 50,
                voiceSearchEnabled = true,
                maintenanceMode = false,
                maintenanceMessage = ""
            )
            saveSettings(currentSettings.copy(appName = appName, footerText = footerText))
            
            val db = FirebaseFirestore.getInstance()
            val providersJson = obj.optJSONArray("providers")
            if (providersJson != null) {
                for (i in 0 until providersJson.length()) {
                    val pObj = providersJson.getJSONObject(i)
                    val id = pObj.getString("id")
                    val fullName = pObj.getString("fullName")
                    val phone = pObj.getString("phone")
                    val mainCategoryId = pObj.getString("mainCategoryId")
                    val address = pObj.getString("address")
                    val p = ProviderEntity(
                        id = id,
                        fullName = fullName,
                        phone = phone,
                        mainCategoryId = mainCategoryId,
                        subCategoryId = "",
                        address = address,
                        district = "",
                        locationLat = 15.3186,
                        locationLng = 44.2045,
                        profileImageUrl = "",
                        idCardImageUrl = "",
                        isPinned = false,
                        isRecommended = false,
                        isVerified = true,
                        isBlocked = false,
                        averageRating = 5.0,
                        totalReviews = 0,
                        isSubscribed = false,
                        subscriptionExpiry = 0L,
                        fcmToken = "",
                        createdAt = System.currentTimeMillis()
                    )
                    db.collection("service_providers").document(p.id).set(p).awaitTask()
                }
            }
            logAction("ADMIN", "استعادة نسخة", "تمت استعادة التهيئة من ملف النسخة الاحتياطية بنجاح")
            true
        } catch (e: Exception) {
            false
        }
    }

    // --- Connected check utility ---
    fun isConnected(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    // --- Gemini Interactive Voice/Text Virtual Assistant ---
    suspend fun getGeminiResponse(userPrompt: String): String = withContext(Dispatchers.IO) {
        val settings = _appSettings.value
        val supportNum = settings?.supportPhone ?: "777644670"

        val promptClean = userPrompt.trim()
        val isOffline = !isConnected()
        
        if (isOffline) {
            return@withContext getOfflineResponse(promptClean, supportNum)
        }

        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext getOfflineResponse(promptClean, supportNum) + " (وضع دون اتصال - لا يوجد مفتاح Gemini معرّف)"
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent?key=$apiKey"
        
        val systemInstructionStr = """
            أنت المساعد الذكي لتطبيق 'دليل خدمات اليمن (WAM)'. تطبيقنا يربط المستخدمين بالمهنيين (سباكين، كهربائيين، دهانين، نجارين، حدادين) في اليمن.
            الرصيد الهاتفي للدعم و التواصل هو $supportNum. أجب بلغة عربية يمنية محببة ومهذبة ومختصرة للغاية. 
            إذا سألك العميل عن الأقسام أو دعم التطبيق أو من المالك، أجب بالتفصيل بناءً على معلومات التطبيق.
        """.trimIndent()

        val jsonRequest = JSONObject().apply {
            val contentsArr = JSONArray().apply {
                val turn = JSONObject().apply {
                    val partsArr = JSONArray().apply {
                        put(JSONObject().apply { put("text", userPrompt) })
                    }
                    put("parts", partsArr)
                }
                put(turn)
            }
            put("contents", contentsArr)
            
            // Note: gemini-pro uses contents style instructions or systemInstruction depending on version. 
            // We can simplify and embed system instruction in contents or use systemInstruction. Both works!
            val systemIns = JSONObject().apply {
                val partsArr = JSONArray().apply {
                    put(JSONObject().apply { put("text", systemInstructionStr) })
                }
                put("parts", partsArr)
            }
            put("systemInstruction", systemIns)

            val genConfig = JSONObject().apply {
                put("temperature", 0.7)
            }
            put("generationConfig", genConfig)
        }

        try {
            val body = jsonRequest.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(url)
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext getOfflineResponse(promptClean, supportNum) + " (فشل اتصال ملقم Gemini)"
                }
                val rawResponse = response.body?.string() ?: ""
                val resObj = JSONObject(rawResponse)
                val candidates = resObj.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val first = candidates.getJSONObject(0)
                    val contentObj = first.getJSONObject("content")
                    val parts = contentObj.getJSONArray("parts")
                    if (parts.length() > 0) {
                        return@withContext parts.getJSONObject(0).getString("text")
                    }
                }
                return@withContext getOfflineResponse(promptClean, supportNum)
            }
        } catch (e: Exception) {
            Log.e("GeminiRequest", "Error requesting Gemini API", e)
            return@withContext getOfflineResponse(promptClean, supportNum) + " (حدث خطأ: ${e.localizedMessage}. تم تشغيل المجيب المحلي)"
        }
    }

    private fun getOfflineResponse(userPrompt: String, supportNum: String): String {
        return when {
            userPrompt.contains("قسم") || userPrompt.contains("اقسام") || userPrompt.contains("الأقسام") -> {
                "أهلاً بك! الأقسام المتوفرة لدينا في دليل اليمن هي:\n" +
                        "🔧 السباكة والتركيب ومواجة المياه\n" +
                        "⚡ الكهرباء وتمديد كابلات وتصليح اللوحات الفنية\n" +
                        "🎨 الدهان والديكور والترميمات الفاخرة\n" +
                        "🔨 النجارة وتثبيت وتركيب الأثاث المنزلي والمكتبي\n" +
                        "⚙️ الحدادة والتصنيع والصيانة الدقيقة ومظلات الحديد\n" +
                        "كل قسم يحتوي على مقدمي خدمات معتمدين مع شارات التوثيق!"
            }
            userPrompt.contains("اتصل") || userPrompt.contains("تواصل") || userPrompt.contains("طريقة") || userPrompt.contains("مقدم") -> {
                "يمكنك الاتصال والدردشة وتنسيق الخدمات مباشرة مع أي مهني بالضغط على بطاقة الفني في الصفحة الرئيسية، ثم اختيار 'اتصال 📞' أو 'واتساب 💬' أو بدء دردشة مجانية فورية داخل التطبيق!"
            }
            userPrompt.contains("دعم") || userPrompt.contains("رقم") || userPrompt.contains("مساعدة") || userPrompt.contains("رقم الدعم") -> {
                "رقم دعم دليل خدمات اليمن هو: $supportNum 📞. يمكنك التواصل معنا في أي وقت لتلقي المساعدة الفورية أو الشكاوي والاقتراحات، أو مراسلتنا عبر الواتساب مباشرة!"
            }
            userPrompt.contains("بلاغ") || userPrompt.contains("أقدم بلاغ") || userPrompt.contains("احتيال") || userPrompt.contains("شكوى") -> {
                "أمان وحقوق مستخدمينا هي أولويتنا رقم 1! لرفع بلاغ ضد فني:\n" +
                        "1️⃣ ادخل إلى تفاصيل حساب المهني\n" +
                        "2️⃣ اضغط على زر 'أقدم بلاغ 🛡️'\n" +
                        "3️⃣ حدد مشكلة الفني واكتب التفاصيل\n" +
                        "يرسل بلاغك فوراً لخلية التدقيق والتحكم لاتخاذ الحظر لغير الملتزمين."
            }
            else -> {
                "مرحباً بك في دليل خدمات اليمن! أنا المساعد الذكي ومستعد للإجابة على جميع تساؤلاتك.\n" +
                        "الأسئلة الشائعة التي يمكنك أن تسألني عنها:\n" +
                        "💡 'ما هي الأقسام المتوفرة؟'\n" +
                        "💡 'كيف أتصل بمقدم خدمة؟'\n" +
                        "💡 'ما هو رقم الدعم الفني؟'\n" +
                        "💡 'كيف أقدم بلاغ على فني؟'\n" +
                        "تفضل بالاستفسار عن أي شيء وسأساعدك فوراً!"
            }
        }
    }
}
