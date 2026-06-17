package com.example.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.*
import com.example.data.repository.AppRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AppRepository(application)

    // --- State Bindings from Repository ---
    val categories: StateFlow<List<CategoryEntity>> = repository.categories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeProviders: StateFlow<List<ProviderEntity>> = repository.activeProviders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allProviders: StateFlow<List<ProviderEntity>> = repository.allProviders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recommendedProviders: StateFlow<List<ProviderEntity>> = repository.recommendedProviders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingProviders: StateFlow<List<PendingProviderEntity>> = repository.pendingProviders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingCount: StateFlow<Int> = repository.pendingCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val appSettings: StateFlow<AppSettingEntity?> = repository.appSettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val activeBanners: StateFlow<List<BannerEntity>> = repository.activeBanners
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allBanners: StateFlow<List<BannerEntity>> = repository.allBanners
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val reports: StateFlow<List<ReportEntity>> = repository.reports
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val reviews: StateFlow<List<ReviewEntity>> = repository.reviews
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val chats: StateFlow<List<ChatEntity>> = repository.chats
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activityLogs: StateFlow<List<ActivityLogEntity>> = repository.activityLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeTerms: StateFlow<List<RegistrationTermEntity>> = repository.activeTerms
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTerms: StateFlow<List<RegistrationTermEntity>> = repository.allTerms
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bookings: StateFlow<List<BookingEntity>> = repository.allBookings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notifications: StateFlow<List<NotificationEntity>> = repository.allNotifications
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Search & Filters State ---
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedCategoryId = MutableStateFlow<String?>(null)
    val selectedCategoryId = _selectedCategoryId.asStateFlow()

    private val _selectedDistrict = MutableStateFlow<String?>(null)
    val selectedDistrict = _selectedDistrict.asStateFlow()

    private val _minRating = MutableStateFlow(0)
    val minRating = _minRating.asStateFlow()

    private val _radiusLimit = MutableStateFlow(10) // in KM
    val radiusLimit = _radiusLimit.asStateFlow()

    private val _currentLanguage = MutableStateFlow("ar")
    val currentLanguage = _currentLanguage.asStateFlow()

    // Combined filtered providers
    val filteredProviders: StateFlow<List<ProviderEntity>> = combine(
        combine(activeProviders, appSettings) { list, settings -> Pair(list, settings) },
        _searchQuery,
        _selectedCategoryId,
        _selectedDistrict,
        _minRating
    ) { (list, settings), query, catId, dstr, rating ->
        val weight = settings?.ratingWeight ?: 1.0
        list.filter { p ->
            val matchesQuery = query.isEmpty() || p.fullName.contains(query, ignoreCase = true) || p.phone.contains(query) || p.address.contains(query, ignoreCase = true) || p.district.contains(query, ignoreCase = true)
            val matchesCategory = catId == null || p.mainCategoryId == catId || p.subCategoryId == catId
            val matchesDistrict = dstr == null || p.district.contains(dstr, ignoreCase = true) || p.address.contains(dstr, ignoreCase = true)
            val matchesRating = p.averageRating >= rating
            matchesQuery && matchesCategory && matchesDistrict && matchesRating
        }.sortedWith(
            compareByDescending<ProviderEntity> { p ->
                if (p.isPinned || p.isSubscribed) 100000.0 else 0.0
            }.thenByDescending { p ->
                (if (p.isVerified) 1000.0 else 0.0) + (p.averageRating * weight)
            }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Auth & Backdoor control states ---
    private val _userRole = MutableStateFlow("Guest") // Guest, Admin, SuperAdmin (backdoor owner)
    val userRole = _userRole.asStateFlow()

    private val _adminUsername = MutableStateFlow("")
    val adminUsername = _adminUsername.asStateFlow()

    private val _backdoorTaps = MutableStateFlow(0)
    val backdoorTaps = _backdoorTaps.asStateFlow()

    // --- Chat Room States ---
    private val _activeChatId = MutableStateFlow<String?>(null)
    val activeChatId = _activeChatId.asStateFlow()

    val currentChatMessages: StateFlow<List<MessageEntity>> = _activeChatId
        .flatMapLatest { chatId ->
            if (chatId == null) flowOf(emptyList()) else repository.getMessagesForChat(chatId)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Gemini Virtual Assistant States ---
    private val _assistantMessages = MutableStateFlow<List<Pair<String, Boolean>>>(
        listOf("أهلاً بك! أنا مساعد دليل خدمات اليمن الذكي لـ WAM. تفضل بسؤالي عن أي شيء وسأجيبك فوراً بكفاءة عالية." to false)
    )
    val assistantMessages = _assistantMessages.asStateFlow()

    private val _isAssistantThinking = MutableStateFlow(false)
    val isAssistantThinking = _isAssistantThinking.asStateFlow()

    // --- Init ---
    init {
        viewModelScope.launch {
            _radiusLimit.value = repository.appSettings.firstOrNull()?.radiusSearchLimit ?: 10
        }
    }

    // --- Filter Handlers ---
    fun onSearchQueryChanged(q: String) {
        _searchQuery.value = q
    }

    fun selectCategory(catId: String?) {
        _selectedCategoryId.value = catId
    }

    fun selectDistrict(district: String?) {
        _selectedDistrict.value = district
    }

    fun selectRating(rating: Int) {
        _minRating.value = rating
    }

    fun toggleLanguage() {
        _currentLanguage.value = if (_currentLanguage.value == "ar") "en" else "ar"
    }

    // --- Backdoor tapping handler ---
    fun registerBackdoorTap(onTrigger: () -> Unit) {
        _backdoorTaps.value += 1
        if (_backdoorTaps.value >= 5) {
            _backdoorTaps.value = 0
            onTrigger()
        }
    }

    // --- User Actions ---
    fun loginAsAdmin(user: String, pass: String, success: () -> Unit, fail: () -> Unit) {
        viewModelScope.launch {
            val settings = repository.appSettings.firstOrNull()
            // Admin logic
            if (user == "WAM2026" && pass == "maher736462") {
                _userRole.value = "Admin"
                _adminUsername.value = "WAM2026"
                repository.logAction("WAM2026", "تسجيل دخول", "المدير العام سجل دخوله للوحة التحكم")
                success()
            } else {
                fail()
            }
        }
    }

    fun loginViaSecretBackdoor(pass: String, success: () -> Unit, fail: () -> Unit) {
        viewModelScope.launch {
            if (pass == "maher--736462") {
                _userRole.value = "SuperAdmin"
                _adminUsername.value = "المالك الفني"
                repository.logAction("OWNER", "بوابة سرية", "تم اجتياز البوابة بنجاح من المالك")
                success()
            } else {
                fail()
            }
        }
    }

    fun logout() {
        _userRole.value = "Guest"
        _adminUsername.value = ""
    }

    // --- Category Management ---
    fun addCategory(arName: String, enName: String, icon: String) {
        viewModelScope.launch {
            val id = "cat_${UUID.randomUUID()}"
            val newCat = CategoryEntity(id, arName, enName, icon, null, 10, true)
            repository.insertCategory(newCat)
        }
    }

    fun deleteCategory(id: String) {
        viewModelScope.launch {
            repository.deleteCategory(id)
        }
    }

    // --- Terms Management ---
    fun addTerm(text: String, order: Int) {
        viewModelScope.launch {
            repository.addRegistrationTerm(text, order)
        }
    }

    fun deleteTerm(id: String) {
        viewModelScope.launch {
            repository.deleteRegistrationTerm(id)
        }
    }

    // --- Provider direct control ---
    fun submitRegistration(
        fullName: String,
        phone: String,
        mainCatId: String,
        subCatId: String,
        address: String,
        district: String,
        profileUri: String,
        idCardUri: String,
        workUris: List<String>,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            val compProfile = compressUriToWebp(context, profileUri)
            val compCard = compressUriToWebp(context, idCardUri)
            val compWorkCSV = workUris.map { compressUriToWebp(context, it) }.filter { it.isNotEmpty() }.joinToString(",")

            repository.submitProviderRegistration(
                fullName = fullName,
                phone = phone,
                mainCatId = mainCatId,
                subCatId = subCatId,
                address = address,
                district = district,
                lat = 15.3186,
                lng = 44.2045,
                profileImageUri = compProfile,
                idCardImageUri = compCard,
                workImagesCSV = compWorkCSV
            )
            onSuccess()
        }
    }

    fun updateProviderDetails(
        id: String,
        fullName: String,
        phone: String,
        address: String,
        district: String,
        profileImageUri: String,
        workImagesCSV: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            val finalProfile = if (profileImageUri.startsWith("content://")) {
                compressUriToWebp(context, profileImageUri)
            } else profileImageUri

            val finalWork = workImagesCSV.split(",")
                .map { uri ->
                    if (uri.startsWith("content://")) compressUriToWebp(context, uri) else uri
                }
                .filter { it.isNotEmpty() }
                .joinToString(",")

            repository.updateProviderDetails(id, fullName, phone, address, district, finalProfile, finalWork)
            onSuccess()
        }
    }

    fun addProviderManually(
        fullName: String,
        phone: String,
        mainCatId: String,
        subCatId: String,
        address: String,
        district: String,
        isVip: Boolean,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            repository.addProviderDirectly(fullName, phone, mainCatId, subCatId, address, district, isVip)
            onSuccess()
        }
    }

    fun approveRegistration(pendingId: String) {
        viewModelScope.launch {
            repository.acceptProviderRegistration(pendingId)
        }
    }

    fun rejectRegistration(pendingId: String, reason: String) {
        viewModelScope.launch {
            repository.rejectProviderRegistration(pendingId, reason)
        }
    }

    fun deleteProviderActive(id: String) {
        viewModelScope.launch {
            repository.deleteProvider(id)
        }
    }

    fun updateFeatures(
        id: String,
        isVerified: Boolean,
        isPinned: Boolean,
        isRecommended: Boolean,
        isSubscribed: Boolean
    ) {
        viewModelScope.launch {
            repository.updateProviderFeatures(id, isVerified, isPinned, isRecommended, isSubscribed)
        }
    }

    // --- Reviews ---
    fun addReview(providerId: String, rate: Int, text: String, userId: String = "user_visitor") {
        viewModelScope.launch {
            repository.submitReview(providerId, userId, rate, text)
        }
    }

    fun removeReview(reviewId: String) {
        viewModelScope.launch {
            repository.deleteReview(reviewId)
        }
    }

    // --- Banners ---
    fun addBannerItem(title: String, type: String, redirect: String, size: String, duration: Int) {
        viewModelScope.launch {
            repository.addBanner(title, type, redirect, size, duration)
        }
    }

    fun deleteBannerItem(id: String) {
        viewModelScope.launch {
            repository.deleteBanner(id)
        }
    }

    // --- User Messaging ---
    fun selectActiveChat(chatId: String) {
        _activeChatId.value = chatId
    }

    fun sendChatMessage(receiverId: String, text: String, senderId: String = "visitor_user") {
        val chatId = _activeChatId.value ?: "chat_session_${receiverId}"
        _activeChatId.value = chatId
        viewModelScope.launch {
            repository.sendMessage(chatId, senderId, receiverId, text)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearAllChatHistory()
        }
    }

    // --- Bookings ---
    fun submitBooking(providerId: String, providerName: String, fullName: String, phone: String, district: String) {
        viewModelScope.launch {
            repository.insertBooking(
                BookingEntity(
                    id = "book_${UUID.randomUUID()}",
                    providerId = providerId,
                    providerName = providerName,
                    fullName = fullName,
                    phone = phone,
                    district = district,
                    status = "pending",
                    createdAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun updateBookingStatus(id: String, status: String) {
        viewModelScope.launch {
            repository.updateBookingStatus(id, status)
        }
    }

    fun deleteBooking(id: String) {
        viewModelScope.launch {
            repository.deleteBooking(id)
        }
    }

    // --- Notifications ---
    fun sendSystemNotification(title: String, body: String, type: String = "general", targetId: String = "") {
        viewModelScope.launch {
            repository.insertNotification(
                NotificationEntity(
                    id = "notif_${UUID.randomUUID()}",
                    title = title,
                    body = body,
                    type = type,
                    targetId = targetId,
                    createdAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun deleteNotification(id: String) {
        viewModelScope.launch {
            repository.deleteNotification(id)
        }
    }

    fun clearAllNotifications() {
        viewModelScope.launch {
            repository.clearAllNotifications()
        }
    }

    // --- Reports ---
    fun submitReportToAdmin(providerId: String, reason: String, details: String) {
        viewModelScope.launch {
            repository.submitReport(providerId, "user_visitor", reason, details)
        }
    }

    fun removeReport(id: String) {
        viewModelScope.launch {
            repository.deleteReport(id)
        }
    }

    // --- AppSettings Modification ---
    fun updateAppSettings(settings: AppSettingEntity) {
        viewModelScope.launch {
            repository.saveSettings(settings)
        }
    }

    // --- Backup & Restore ---
    fun performBackup(callback: (String) -> Unit) {
        viewModelScope.launch {
            val cacheDir = getApplication<Application>().cacheDir.absolutePath
            val file = repository.backupDatabaseToFile(cacheDir)
            callback(file.absolutePath)
        }
    }

    fun performRestore(filePath: String, success: () -> Unit, fail: () -> Unit) {
        viewModelScope.launch {
            val file = File(filePath)
            if (file.exists()) {
                val ok = repository.restoreDatabaseFromFile(file)
                if (ok) success() else fail()
            } else {
                fail()
            }
        }
    }

    // --- Gemini Virtual Assistant ---
    fun askAssistant(prompt: String) {
        if (prompt.trim().isEmpty()) return
        
        // Append user prompt
        _assistantMessages.value = _assistantMessages.value + (prompt to true)
        _isAssistantThinking.value = true

        viewModelScope.launch {
            val reply = repository.getGeminiResponse(prompt)
            _assistantMessages.value = _assistantMessages.value + (reply to false)
            _isAssistantThinking.value = false
        }
    }

    fun isConnected(): Boolean = repository.isConnected()

    private fun compressUriToWebp(context: android.content.Context, uriString: String): String {
        if (uriString.isEmpty()) return ""
        return try {
            val uri = Uri.parse(uriString)
            val inputStream = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close() ?: return uriString

            val outDir = File(context.filesDir, "compressed_images")
            if (!outDir.exists()) outDir.mkdirs()

            val outFile = File(outDir, "img_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.webp")
            val outStream = FileOutputStream(outFile)

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, 75, outStream)
            } else {
                @Suppress("DEPRECATION")
                bitmap.compress(Bitmap.CompressFormat.WEBP, 75, outStream)
            }
            outStream.flush()
            outStream.close()
            outFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            uriString
        }
    }
}

class AppViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AppViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AppViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
