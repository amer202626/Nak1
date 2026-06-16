package com.dalyly

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

// --- Data Models ---
data class CategoryEntity(
    val id: String = "",
    val nameAr: String = "",
    val nameEn: String = "",
    val imageUrl: String? = "🔧",
    val parentId: String? = null,
    val isEnabled: Boolean = true,
    val displayOrder: Int = 0
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "nameAr" to nameAr,
        "nameEn" to nameEn,
        "imageUrl" to imageUrl,
        "parentId" to parentId,
        "isEnabled" to isEnabled,
        "displayOrder" to displayOrder
    )
}

data class ProviderEntity(
    val id: String = "",
    val fullName: String = "",
    val phone: String = "",
    val mainCategoryId: String = "",
    val subCategoryId: String = "",
    val district: String = "",
    val address: String = "",
    val locationLat: Double = 15.3186,
    val locationLng: Double = 44.2045,
    val averageRating: Double = 5.0,
    val ratingCount: Int = 1,
    val isVerified: Boolean = false,
    val isPinned: Boolean = false,
    val isRecommended: Boolean = false, // VIP Status
    val isSubscribed: Boolean = false
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "fullName" to fullName,
        "phone" to phone,
        "mainCategoryId" to mainCategoryId,
        "subCategoryId" to subCategoryId,
        "district" to district,
        "address" to address,
        "locationLat" to locationLat,
        "locationLng" to locationLng,
        "averageRating" to averageRating,
        "ratingCount" to ratingCount,
        "isVerified" to isVerified,
        "isPinned" to isPinned,
        "isRecommended" to isRecommended,
        "isSubscribed" to isSubscribed
    )
}

data class RegistrationEntity(
    val id: String = "",
    val fullName: String = "",
    val phone: String = "",
    val mainCategoryId: String = "",
    val subCategoryId: String = "",
    val district: String = "",
    val address: String = "",
    val locationLat: Double = 15.3186,
    val locationLng: Double = 44.2045
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "fullName" to fullName,
        "phone" to phone,
        "mainCategoryId" to mainCategoryId,
        "subCategoryId" to subCategoryId,
        "district" to district,
        "address" to address,
        "locationLat" to locationLat,
        "locationLng" to locationLng
    )
}

data class ReviewEntity(
    val id: String = "",
    val providerId: String = "",
    val reviewerName: String = "",
    val rating: Int = 5,
    val comment: String = "",
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "providerId" to providerId,
        "reviewerName" to reviewerName,
        "rating" to rating,
        "comment" to comment,
        "timestamp" to timestamp
    )
}

data class ReportEntity(
    val id: String = "",
    val providerId: String = "",
    val reason: String = "",
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "providerId" to providerId,
        "reason" to reason,
        "timestamp" to timestamp
    )
}

data class AppSettingEntity(
    val id: String = "global",
    val supportPhone: String = "+967770000000",
    val supportEmail: String = "support@dalyly.com",
    val supportWhatsApp: String = "https://wa.me/967770000000",
    val fontSize: Float = 1.0f,
    val radiusSearchLimit: Int = 20,
    val voiceSearchEnabled: Boolean = true,
    val isMapEnabled: Boolean = true,
    val ownerPassword: String = "maher736462",
    val distributionMode: Int = 1,
    val loyaltyPointsEnabled: Boolean = true,
    val isMandatoryRegistration: Boolean = false,
    val coverPhotoUrl: String = "",
    val smartAssistantEnabled: Boolean = true,
    val appThemeColor: String = "default"
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "supportPhone" to supportPhone,
        "supportEmail" to supportEmail,
        "supportWhatsApp" to supportWhatsApp,
        "fontSize" to fontSize,
        "radiusSearchLimit" to radiusSearchLimit,
        "voiceSearchEnabled" to voiceSearchEnabled,
        "isMapEnabled" to isMapEnabled,
        "ownerPassword" to ownerPassword,
        "distributionMode" to distributionMode,
        "loyaltyPointsEnabled" to loyaltyPointsEnabled,
        "isMandatoryRegistration" to isMandatoryRegistration,
        "coverPhotoUrl" to coverPhotoUrl,
        "smartAssistantEnabled" to smartAssistantEnabled,
        "appThemeColor" to appThemeColor
    )
}

data class BookingEntity(
    val id: String = "",
    val fullName: String = "",
    val phone: String = "",
    val serviceCategory: String = "",
    val district: String = "",
    val notes: String = "",
    val providerId: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "قيد الانتظار",
    val assignedProviderId: String? = null,
    val customInputs: Map<String, String> = emptyMap()
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "fullName" to fullName,
        "phone" to phone,
        "serviceCategory" to serviceCategory,
        "district" to district,
        "notes" to notes,
        "providerId" to providerId,
        "timestamp" to timestamp,
        "status" to status,
        "assignedProviderId" to assignedProviderId,
        "customInputs" to customInputs
    )
}

data class FormFieldEntity(
    val id: String = "",
    val labelAr: String = "",
    val labelEn: String = "",
    val type: String = "text",
    val isRequired: Boolean = true,
    val options: List<String> = emptyList(),
    val displayOrder: Int = 0
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "labelAr" to labelAr,
        "labelEn" to labelEn,
        "type" to type,
        "isRequired" to isRequired,
        "options" to options,
        "displayOrder" to displayOrder
    )
}

data class NotificationConfigEntity(
    val id: String = "",
    val reason: String = "",
    val titleAr: String = "",
    val titleEn: String = "",
    val bodyTemplateAr: String = "",
    val bodyTemplateEn: String = "",
    val isEnabled: Boolean = true
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "reason" to reason,
        "titleAr" to titleAr,
        "titleEn" to titleEn,
        "bodyTemplateAr" to bodyTemplateAr,
        "bodyTemplateEn" to bodyTemplateEn,
        "isEnabled" to isEnabled
    )
}

data class NotificationLogEntity(
    val id: String = "",
    val title: String = "",
    val body: String = "",
    val imageOrLink: String? = null,
    val recipient: String = "all",
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "title" to title,
        "body" to body,
        "imageOrLink" to imageOrLink,
        "recipient" to recipient,
        "timestamp" to timestamp,
        "isRead" to isRead
    )
}

data class AdminSupervisorEntity(
    val id: String = "",
    val username: String = "",
    val passcode: String = "",
    val roleType: String = "supervisor",
    val supervisedCategoryId: String? = null
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "username" to username,
        "passcode" to passcode,
        "roleType" to roleType,
        "supervisedCategoryId" to supervisedCategoryId
    )
}

data class ActivityLogEntity(
    val id: String = "",
    val actor: String = "",
    val actionAr: String = "",
    val actionEn: String = "",
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "actor" to actor,
        "actionAr" to actionAr,
        "actionEn" to actionEn,
        "timestamp" to timestamp
    )
}

data class AdCampaignEntity(
    val id: String = "",
    val title: String = "",
    val desc: String = "",
    val image: String = "",
    val price: Double = 0.0,
    val activeDays: Int = 30
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "title" to title,
        "desc" to desc,
        "image" to image,
        "price" to price,
        "activeDays" to activeDays
    )
}

data class CouponEntity(
    val code: String = "",
    val discountPercent: Int = 0,
    val isValid: Boolean = true
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "code" to code,
        "discountPercent" to discountPercent,
        "isValid" to isValid
    )
}

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val firestore = FirebaseFirestore.getInstance()
    private val prefs = application.getSharedPreferences("WAM_SETTINGS", Context.MODE_PRIVATE)

    // --- StateFlows ---
    private val _categories = MutableStateFlow<List<CategoryEntity>>(emptyList())
    val categories: StateFlow<List<CategoryEntity>> = _categories.asStateFlow()

    private val _providers = MutableStateFlow<List<ProviderEntity>>(emptyList())
    val providers: StateFlow<List<ProviderEntity>> = _providers.asStateFlow()

    private val _registrations = MutableStateFlow<List<RegistrationEntity>>(emptyList())
    val registrations: StateFlow<List<RegistrationEntity>> = _registrations.asStateFlow()

    private val _settings = MutableStateFlow<AppSettingEntity>(AppSettingEntity())
    val settings: StateFlow<AppSettingEntity> = _settings.asStateFlow()

    private val _reviews = MutableStateFlow<List<ReviewEntity>>(emptyList())
    val reviews: StateFlow<List<ReviewEntity>> = _reviews.asStateFlow()

    private val _reports = MutableStateFlow<List<ReportEntity>>(emptyList())
    val reports: StateFlow<List<ReportEntity>> = _reports.asStateFlow()

    private val _bookings = MutableStateFlow<List<BookingEntity>>(emptyList())
    val bookings: StateFlow<List<BookingEntity>> = _bookings.asStateFlow()

    private val _bookingFields = MutableStateFlow<List<FormFieldEntity>>(emptyList())
    val bookingFields: StateFlow<List<FormFieldEntity>> = _bookingFields.asStateFlow()

    private val _adminSupervisors = MutableStateFlow<List<AdminSupervisorEntity>>(emptyList())
    val adminSupervisors: StateFlow<List<AdminSupervisorEntity>> = _adminSupervisors.asStateFlow()

    private val _notificationConfigs = MutableStateFlow<List<NotificationConfigEntity>>(emptyList())
    val notificationConfigs: StateFlow<List<NotificationConfigEntity>> = _notificationConfigs.asStateFlow()

    private val _notificationLogs = MutableStateFlow<List<NotificationLogEntity>>(emptyList())
    val notificationLogs: StateFlow<List<NotificationLogEntity>> = _notificationLogs.asStateFlow()

    private val _activityLogs = MutableStateFlow<List<ActivityLogEntity>>(emptyList())
    val activityLogs: StateFlow<List<ActivityLogEntity>> = _activityLogs.asStateFlow()

    private val _coupons = MutableStateFlow<List<CouponEntity>>(emptyList())
    val coupons: StateFlow<List<CouponEntity>> = _coupons.asStateFlow()

    private val _adCampaigns = MutableStateFlow<List<AdCampaignEntity>>(emptyList())
    val adCampaigns: StateFlow<List<AdCampaignEntity>> = _adCampaigns.asStateFlow()

    // --- UI Filters Local State ---
    private val _selectedCategoryId = MutableStateFlow<String?>(null)
    val selectedCategoryId: StateFlow<String?> = _selectedCategoryId.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedDistrict = MutableStateFlow<String?>(null)
    val selectedDistrict: StateFlow<String?> = _selectedDistrict.asStateFlow()

    private val _minRatingFilter = MutableStateFlow(0)
    val minRatingFilter: StateFlow<Int> = _minRatingFilter.asStateFlow()

    private val _language = MutableStateFlow("ar") // "ar" or "en"
    val language: StateFlow<String> = _language.asStateFlow()

    init {
        // Fetch saved language of app locally
        _language.value = prefs.getString("APP_LANG", "ar") ?: "ar"
        
        setupFirestoreSnapshotListeners()
    }

    private fun setupFirestoreSnapshotListeners() {
        // 1. Categories Live Listening
        firestore.collection("categories")
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val list = ArrayList<CategoryEntity>()
                snapshot?.documents?.forEach { doc ->
                    list.add(doc.toCategoryEntity())
                }
                
                // If the dynamic collection is empty, automatically seed initial default categories
                if (list.isEmpty()) {
                    seedDefaultCategories()
                } else {
                    _categories.value = list
                }
            }

        // 2. Providers Live Listening
        firestore.collection("providers")
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val list = ArrayList<ProviderEntity>()
                snapshot?.documents?.forEach { doc ->
                    list.add(doc.toProviderEntity())
                }
                _providers.value = list
            }

        // 3. Registrations Live Listening
        firestore.collection("registrations")
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val list = ArrayList<RegistrationEntity>()
                snapshot?.documents?.forEach { doc ->
                    list.add(doc.toRegistrationEntity())
                }
                _registrations.value = list
            }

        // 4. Reviews Live Listening
        firestore.collection("reviews")
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val list = ArrayList<ReviewEntity>()
                snapshot?.documents?.forEach { doc ->
                    list.add(doc.toReviewEntity())
                }
                _reviews.value = list
            }

        // 5. Reports Live Listening
        firestore.collection("reports")
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val list = ArrayList<ReportEntity>()
                snapshot?.documents?.forEach { doc ->
                    list.add(doc.toReportEntity())
                }
                _reports.value = list
            }

        // 6. Settings Live Listening
        firestore.collection("settings").document("global")
            .addSnapshotListener { doc, error ->
                if (error != null) return@addSnapshotListener
                if (doc != null && doc.exists()) {
                    _settings.value = doc.toAppSettingEntity()
                } else {
                    // Seed initial global settings
                    seedDefaultSettings()
                }
            }

        // 7. Bookings Live Listening
        firestore.collection("bookings")
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val list = ArrayList<BookingEntity>()
                snapshot?.documents?.forEach { doc ->
                    list.add(doc.toBookingEntity())
                }
                _bookings.value = list
            }

        // 8. Custom Booking Fields Live Listening
        firestore.collection("booking_fields")
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val list = ArrayList<FormFieldEntity>()
                snapshot?.documents?.forEach { doc ->
                    list.add(doc.toFormFieldEntity())
                }
                if (list.isEmpty()) {
                    seedDefaultBookingFields()
                } else {
                    _bookingFields.value = list.sortedBy { it.displayOrder }
                }
            }

        // 9. Admin Supervisors Live Listening
        firestore.collection("admin_supervisors")
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val list = ArrayList<AdminSupervisorEntity>()
                snapshot?.documents?.forEach { doc ->
                    list.add(doc.toAdminSupervisorEntity())
                }
                if (list.isEmpty()) {
                    seedDefaultSupervisors()
                } else {
                    _adminSupervisors.value = list
                }
            }

        // 10. Notification Configs Live Listening
        firestore.collection("notifications_config")
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val list = ArrayList<NotificationConfigEntity>()
                snapshot?.documents?.forEach { doc ->
                    list.add(doc.toNotificationConfigEntity())
                }
                if (list.isEmpty()) {
                    seedDefaultNotificationConfigs()
                } else {
                    _notificationConfigs.value = list
                }
            }

        // 11. Notification Logs Live Listening
        firestore.collection("notifications_log")
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val list = ArrayList<NotificationLogEntity>()
                snapshot?.documents?.forEach { doc ->
                    list.add(doc.toNotificationLogEntity())
                }
                _notificationLogs.value = list.sortedByDescending { it.timestamp }
            }

        // 12. Activity Logs Live Listening
        firestore.collection("activity_logs")
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val list = ArrayList<ActivityLogEntity>()
                snapshot?.documents?.forEach { doc ->
                    list.add(doc.toActivityLogEntity())
                }
                _activityLogs.value = list.sortedByDescending { it.timestamp }
            }

        // 13. Coupons Live Listening
        firestore.collection("coupons")
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val list = ArrayList<CouponEntity>()
                snapshot?.documents?.forEach { doc ->
                    list.add(doc.toCouponEntity())
                }
                if (list.isEmpty()) {
                    seedDefaultCoupons()
                } else {
                    _coupons.value = list
                }
            }

        // 14. Support Campaingns Live Listening
        firestore.collection("ad_campaigns")
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val list = ArrayList<AdCampaignEntity>()
                snapshot?.documents?.forEach { doc ->
                    list.add(doc.toAdCampaignEntity())
                }
                if (list.isEmpty()) {
                    seedDefaultAdCampaigns()
                } else {
                    _adCampaigns.value = list
                }
            }
    }

    // --- Seeding Operations ---
    private fun seedDefaultCategories() {
        val initialCategories = listOf(
            CategoryEntity("c1", "سباكة وصيانة صحية", "Plumbing & Sanitary", "🔧"),
            CategoryEntity("c2", "كهرباء وتمديدات", "Electrical & Wiring", "⚡"),
            CategoryEntity("c3", "دهانات ونقوش جدران", "Painting & Wallpaper", "🎨"),
            CategoryEntity("c4", "نجارة وأخشاب", "Carpentry & Woods", "🔨"),
            CategoryEntity("c5", "تكييف وتبريد وأجهزة", "AC & Appliance Repair", "⚙️")
        )
        viewModelScope.launch {
            initialCategories.forEach { cat ->
                firestore.collection("categories").document(cat.id).set(cat.toMap())
            }
        }
    }

    private fun seedDefaultSettings() {
        val def = AppSettingEntity()
        firestore.collection("settings").document("global").set(def.toMap())
    }

    private fun seedDefaultBookingFields() {
        val fields = listOf(
            FormFieldEntity("f1", "الاسم الكامل", "Full Name", "text", true, emptyList(), 1),
            FormFieldEntity("f2", "رقم الهاتف", "Phone Number", "number", true, emptyList(), 2),
            FormFieldEntity("f3", "الخدمة المطلوبة", "Requested Service", "dropdown", true, emptyList(), 3),
            FormFieldEntity("f4", "المنطقة", "District Area", "dropdown", true, listOf("صنعاء", "عدن", "تعز", "حضرموت", "إب", "الكل"), 4),
            FormFieldEntity("f5", "ملاحظات إضافية", "Additional Notes", "text", false, emptyList(), 5)
        )
        viewModelScope.launch {
            fields.forEach { firestore.collection("booking_fields").document(it.id).set(it.toMap()) }
        }
    }

    private fun seedDefaultSupervisors() {
        val list = listOf(
            AdminSupervisorEntity("s1", "أحمد السباك", "123", "supervisor", "c1"),
            AdminSupervisorEntity("s2", "صالح الكهربائي", "123", "supervisor", "c2")
        )
        viewModelScope.launch {
            list.forEach { firestore.collection("admin_supervisors").document(it.id).set(it.toMap()) }
        }
    }

    private fun seedDefaultNotificationConfigs() {
        val list = listOf(
            NotificationConfigEntity("1", "حجز جديد معلق", "حجز جديد معلق", "New pending booking", "هناك غرض حجز جديد بانتظار التعامل كفء.", "There is a new pending booking request.", true),
            NotificationConfigEntity("2", "توزيع حجز على فني", "تم تعيين حجز جديد لك", "New booking assigned", "المشرف قام بتعيين طلب جديد لك تمكين.", "Supervisor has assigned a new booking to you.", true),
            NotificationConfigEntity("3", "فني يقبل حجزاً", "تم قبول طلب حجزك", "Booking accepted", "تم قبول حجزك من قبل مهني الخدمة بنجاح.", "Your booking request is accepted by professional.", true),
            NotificationConfigEntity("4", "فني يغير حالة حجز قيد التنفيذ", "الخدمة قيد التنفيذ", "Service in progress", "بدأ مهني الخدمة تنفيذ طلبك الآن بكل همة.", "The service is currently being executed.", true),
            NotificationConfigEntity("5", "فني ينهي الخدمة مكتمل", "تم انتهاء العمل بنجاح مكتمل", "Service completion", "تم إنهاء العمل وتقديم الخدمة بتمام الصحة.", "Active service completed successfully.", true),
            NotificationConfigEntity("6", "أدمن يغير حالة حجز", "تحديث حالة الحجز", "Booking status update", "تم تغيير حالة الحجز من قبل المشرف العام.", "Booking status was updated by administrative supervisor.", true),
            NotificationConfigEntity("7", "فني جديد يسجل وينتظر الموافقة", "طلب تسجيل مهني جديد", "New professional registration", "هناك طلب تسجيل فني جديد بانتظار المراجعة والتمحيص.", "A new service provider is waiting for admission approval.", true),
            NotificationConfigEntity("8", "قبول طلب تسجيل فني", "تم قبول انضمامك للدليل", "Admission approved", "تهانينا! تم تفعيل حسابك كفني معتمد في دليل كل خدمات اليمن.", "Incredible! Your active service card has been certified.", true),
            NotificationConfigEntity("9", "رفض طلب تسجيل فني", "تم رفض طلب الانضمام", "Admission declined", "عذراً، تم تقديم طلب انضامك للتدقيق والخبرة الفنية لكنه رُفض.", "Declined due to credentials issues or details mismatch.", true),
            NotificationConfigEntity("10", "مزامنة فاشلة", "تنبيه مزامنة لقاعدة البيانات", "Sync warning alert", "واجه النظام عائقاً فنياً أثناء مزامنة البيانات السحابية.", "Cloud sync database experienced a slight interruption.", true),
            NotificationConfigEntity("11", "إشعار جماعي يدوي", "تنبيه هام من الإدارة", "Administrative broadcast", "رسالة عامة هامة موجهة لجميع مستخدمي التطبيق الكرام.", "Notification broadcast from management portal.", true),
            NotificationConfigEntity("12", "تذكير بحجز مؤكد", "تذكير بقرب موعد الخدمة", "Booking schedule reminder", "نود تذكيرك بالخدمة المجدولة قريباً.", "Scheduled booking service appointment reminder.", true),
            NotificationConfigEntity("13", "كوبون خصم جديد", "خصومات وهدايا ترحيبية", "New promo offer", "احصل على كود خصم تفعيلي خاص للأنشطة المختلفة باليمن السعيد.", "A promo discount coupon has been introduced.", true)
        )
        viewModelScope.launch {
            list.forEach { firestore.collection("notifications_config").document(it.id).set(it.toMap()) }
        }
    }

    private fun seedDefaultCoupons() {
        val list = listOf(
            CouponEntity("YEMEN2026", 15, true),
            CouponEntity("WELCOME10", 10, true)
        )
        viewModelScope.launch {
            list.forEach { firestore.collection("coupons").document(it.code).set(it.toMap()) }
        }
    }

    private fun seedDefaultAdCampaigns() {
        val list = listOf(
            AdCampaignEntity("p1", "الباقة المميزة الفضية", "شريحة إعلانات الترويج المهنية", "", 10000.0, 15),
            AdCampaignEntity("p2", "الباقة الذهبية للشركات", "تثبيت ممتد في التوصية الكبرى", "", 25000.0, 30)
        )
        viewModelScope.launch {
            list.forEach { firestore.collection("ad_campaigns").document(it.id).set(it.toMap()) }
        }
    }

    // --- Search & Filtering Controls ---
    fun selectCategory(catId: String?) {
        _selectedCategoryId.value = catId
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun selectDistrict(dist: String?) {
        _selectedDistrict.value = dist
    }

    fun setMinRating(rating: Int) {
        _minRatingFilter.value = rating
    }

    fun toggleLanguage() {
        val newLang = if (_language.value == "ar") "en" else "ar"
        _language.value = newLang
        prefs.edit().putString("APP_LANG", newLang).apply()
    }

    // --- Category CRUD Writes ---
    fun addCategory(ar: String, en: String, icon: String) {
        val id = UUID.randomUUID().toString()
        val cat = CategoryEntity(id, ar, en, icon, null)
        firestore.collection("categories").document(id).set(cat.toMap())
    }

    fun editCategory(cat: CategoryEntity) {
        firestore.collection("categories").document(cat.id).set(cat.toMap())
    }

    fun deleteCategory(id: String) {
        firestore.collection("categories").document(id).delete()
    }

    // --- Registration Forms ---
    fun registerProfessional(
        fullName: String,
        phone: String,
        catId: String,
        district: String,
        address: String,
        lat: Double,
        lng: Double
    ) {
        val id = UUID.randomUUID().toString()
        val reg = RegistrationEntity(
            id = id,
            fullName = fullName,
            phone = phone,
            mainCategoryId = catId,
            district = district,
            address = address,
            locationLat = lat,
            locationLng = lng
        )
        firestore.collection("registrations").document(id).set(reg.toMap())
    }

    // --- Active Provider Administration CRUD ---
    fun approveRegistration(reg: RegistrationEntity) {
        // Move registration to approved providers
        val p = ProviderEntity(
            id = reg.id,
            fullName = reg.fullName,
            phone = reg.phone,
            mainCategoryId = reg.mainCategoryId,
            district = reg.district,
            address = reg.address,
            locationLat = reg.locationLat,
            locationLng = reg.locationLng,
            isVerified = true,
            isRecommended = false,
            isSubscribed = false
        )
        firestore.collection("providers").document(p.id).set(p.toMap())
        firestore.collection("registrations").document(reg.id).delete()
    }

    fun deleteRegistration(id: String) {
        firestore.collection("registrations").document(id).delete()
    }

    fun deleteProviderActive(id: String) {
        firestore.collection("providers").document(id).delete()
    }

    fun editProviderDetails(provider: ProviderEntity) {
        firestore.collection("providers").document(provider.id).set(provider.toMap())
    }

    fun updateFeatures(id: String, verified: Boolean, pinned: Boolean, recommended: Boolean, subscribed: Boolean) {
        firestore.collection("providers").document(id).update(
            "isVerified", verified,
            "isPinned", pinned,
            "isRecommended", recommended,
            "isSubscribed", subscribed
        )
    }

    // --- Reviews & Reports Operations ---
    fun postReview(providerId: String, name: String, stars: Int, comment: String) {
        val id = UUID.randomUUID().toString()
        val rev = ReviewEntity(id, providerId, name, stars, comment)
        firestore.collection("reviews").document(id).set(rev.toMap())

        // Calculate and update average rating in the target provider doc
        viewModelScope.launch {
            val providerReviews = _reviews.value.filter { it.providerId == providerId } + rev
            val avg = providerReviews.map { it.rating }.average()
            val finalAvg = if (avg.isNaN()) 5.0 else String.format("%.1f", avg).toDouble()
            
            firestore.collection("providers").document(providerId).update(
                "averageRating", finalAvg,
                "ratingCount", providerReviews.size
            )
        }
    }

    fun postReport(providerId: String, reason: String) {
        val id = UUID.randomUUID().toString()
        val rep = ReportEntity(id, providerId, reason)
        firestore.collection("reports").document(id).set(rep.toMap())
    }

    // --- Global App Settings Save ---
    fun updateAppSettings(settings: AppSettingEntity) {
        firestore.collection("settings").document("global").set(settings.toMap())
    }

    fun syncFirestoreLive() {
        // This is a realtime snapshot listener based VM, so updates are naturally and immediately synced.
        // We can just trigger an empty update to trigger listener re-check
        firestore.collection("settings").document("global").get()
    }

    // --- Extension Map Parsers for Firestore DocumentSnapshots ---
    private fun DocumentSnapshot.toCategoryEntity(): CategoryEntity = CategoryEntity(
        id = getString("id") ?: id,
        nameAr = getString("nameAr") ?: "",
        nameEn = getString("nameEn") ?: "",
        imageUrl = getString("imageUrl") ?: "🔧",
        parentId = getString("parentId"),
        isEnabled = getBoolean("isEnabled") ?: true,
        displayOrder = getLong("displayOrder")?.toInt() ?: 0
    )

    private fun DocumentSnapshot.toProviderEntity(): ProviderEntity = ProviderEntity(
        id = getString("id") ?: id,
        fullName = getString("fullName") ?: "",
        phone = getString("phone") ?: "",
        mainCategoryId = getString("mainCategoryId") ?: "",
        subCategoryId = getString("subCategoryId") ?: "",
        district = getString("district") ?: "",
        address = getString("address") ?: "",
        locationLat = getDouble("locationLat") ?: 15.3186,
        locationLng = getDouble("locationLng") ?: 44.2045,
        averageRating = getDouble("averageRating") ?: 5.0,
        ratingCount = getLong("ratingCount")?.toInt() ?: 1,
        isVerified = getBoolean("isVerified") ?: false,
        isPinned = getBoolean("isPinned") ?: false,
        isRecommended = getBoolean("isRecommended") ?: false,
        isSubscribed = getBoolean("isSubscribed") ?: false
    )

    private fun DocumentSnapshot.toRegistrationEntity(): RegistrationEntity = RegistrationEntity(
        id = getString("id") ?: id,
        fullName = getString("fullName") ?: "",
        phone = getString("phone") ?: "",
        mainCategoryId = getString("mainCategoryId") ?: "",
        subCategoryId = getString("subCategoryId") ?: "",
        district = getString("district") ?: "",
        address = getString("address") ?: "",
        locationLat = getDouble("locationLat") ?: 15.3186,
        locationLng = getDouble("locationLng") ?: 44.2045
    )

    private fun DocumentSnapshot.toReviewEntity(): ReviewEntity = ReviewEntity(
        id = getString("id") ?: id,
        providerId = getString("providerId") ?: "",
        reviewerName = getString("reviewerName") ?: "",
        rating = getLong("rating")?.toInt() ?: 5,
        comment = getString("comment") ?: "",
        timestamp = getLong("timestamp") ?: System.currentTimeMillis()
    )

    private fun DocumentSnapshot.toReportEntity(): ReportEntity = ReportEntity(
        id = getString("id") ?: id,
        providerId = getString("providerId") ?: "",
        reason = getString("reason") ?: "",
        timestamp = getLong("timestamp") ?: System.currentTimeMillis()
    )

    private fun DocumentSnapshot.toAppSettingEntity(): AppSettingEntity = AppSettingEntity(
        id = id,
        supportPhone = getString("supportPhone") ?: "+967770000000",
        supportEmail = getString("supportEmail") ?: "support@dalyly.com",
        supportWhatsApp = getString("supportWhatsApp") ?: "https://wa.me/967770000000",
        fontSize = getDouble("fontSize")?.toFloat() ?: 1.0f,
        radiusSearchLimit = getLong("radiusSearchLimit")?.toInt() ?: 20,
        voiceSearchEnabled = getBoolean("voiceSearchEnabled") ?: true,
        isMapEnabled = getBoolean("isMapEnabled") ?: true,
        ownerPassword = getString("ownerPassword") ?: "maher736462",
        distributionMode = getLong("distributionMode")?.toInt() ?: 1,
        loyaltyPointsEnabled = getBoolean("loyaltyPointsEnabled") ?: true,
        isMandatoryRegistration = getBoolean("isMandatoryRegistration") ?: false,
        coverPhotoUrl = getString("coverPhotoUrl") ?: "",
        smartAssistantEnabled = getBoolean("smartAssistantEnabled") ?: true,
        appThemeColor = getString("appThemeColor") ?: "default"
    )

    private fun DocumentSnapshot.toBookingEntity(): BookingEntity {
        @Suppress("UNCHECKED_CAST")
        val inputs = (get("customInputs") as? Map<String, Any>)?.mapValues { it.value.toString() } ?: emptyMap()
        return BookingEntity(
            id = getString("id") ?: id,
            fullName = getString("fullName") ?: "",
            phone = getString("phone") ?: "",
            serviceCategory = getString("serviceCategory") ?: "",
            district = getString("district") ?: "",
            notes = getString("notes") ?: "",
            providerId = getString("providerId"),
            timestamp = getLong("timestamp") ?: System.currentTimeMillis(),
            status = getString("status") ?: "قيد الانتظار",
            assignedProviderId = getString("assignedProviderId"),
            customInputs = inputs
        )
    }

    private fun DocumentSnapshot.toFormFieldEntity(): FormFieldEntity {
        @Suppress("UNCHECKED_CAST")
        val opt = get("options") as? List<String> ?: emptyList()
        return FormFieldEntity(
            id = getString("id") ?: id,
            labelAr = getString("labelAr") ?: "",
            labelEn = getString("labelEn") ?: "",
            type = getString("type") ?: "text",
            isRequired = getBoolean("isRequired") ?: true,
            options = opt,
            displayOrder = getLong("displayOrder")?.toInt() ?: 0
        )
    }

    private fun DocumentSnapshot.toAdminSupervisorEntity(): AdminSupervisorEntity = AdminSupervisorEntity(
        id = getString("id") ?: id,
        username = getString("username") ?: "",
        passcode = getString("passcode") ?: "",
        roleType = getString("roleType") ?: "supervisor",
        supervisedCategoryId = getString("supervisedCategoryId")
    )

    private fun DocumentSnapshot.toNotificationConfigEntity(): NotificationConfigEntity = NotificationConfigEntity(
        id = getString("id") ?: id,
        reason = getString("reason") ?: "",
        titleAr = getString("titleAr") ?: "",
        titleEn = getString("titleEn") ?: "",
        bodyTemplateAr = getString("bodyTemplateAr") ?: "",
        bodyTemplateEn = getString("bodyTemplateEn") ?: "",
        isEnabled = getBoolean("isEnabled") ?: true
    )

    private fun DocumentSnapshot.toNotificationLogEntity(): NotificationLogEntity = NotificationLogEntity(
        id = getString("id") ?: id,
        title = getString("title") ?: "",
        body = getString("body") ?: "",
        imageOrLink = getString("imageOrLink"),
        recipient = getString("recipient") ?: "all",
        timestamp = getLong("timestamp") ?: System.currentTimeMillis(),
        isRead = getBoolean("isRead") ?: false
    )

    private fun DocumentSnapshot.toActivityLogEntity(): ActivityLogEntity = ActivityLogEntity(
        id = getString("id") ?: id,
        actor = getString("actor") ?: "",
        actionAr = getString("actionAr") ?: "",
        actionEn = getString("actionEn") ?: "",
        timestamp = getLong("timestamp") ?: System.currentTimeMillis()
    )

    private fun DocumentSnapshot.toCouponEntity(): CouponEntity = CouponEntity(
        code = getString("code") ?: id,
        discountPercent = getLong("discountPercent")?.toInt() ?: 0,
        isValid = getBoolean("isValid") ?: true
    )

    private fun DocumentSnapshot.toAdCampaignEntity(): AdCampaignEntity = AdCampaignEntity(
        id = getString("id") ?: id,
        title = getString("title") ?: "",
        desc = getString("desc") ?: "",
        image = getString("image") ?: "",
        price = getDouble("price") ?: 0.0,
        activeDays = getLong("activeDays")?.toInt() ?: 30
    )

    // --- Booking Administration Operations ---
    fun createBooking(fullName: String, phone: String, categoryId: String, district: String, notes: String, providerId: String?, customInputs: Map<String, String>) {
        val bId = UUID.randomUUID().toString()
        val booking = BookingEntity(
            id = bId,
            fullName = fullName,
            phone = phone,
            serviceCategory = categoryId,
            district = district,
            notes = notes,
            providerId = providerId,
            status = "قيد الانتظار",
            customInputs = customInputs
        )
        firestore.collection("bookings").document(bId).set(booking.toMap())
        triggerNotification("1", "العميل: $fullName - القسم: $categoryId", null, "admin")
        logSystemAction("العميل / الزائر", "إنشاء حجز جديد رقم #${bId.take(6)}", "Created billing booking #${bId.take(6)}")
    }

    fun updateBookingStatus(bookingId: String, newStatus: String, actorName: String) {
        firestore.collection("bookings").document(bookingId).update("status", newStatus)
        val notifyReasonId = when (newStatus) {
            "تم القبول" -> "3"
            "قيد التنفيذ" -> "4"
            "مكتمل" -> "5"
            "ملغي" -> "6"
            else -> "6"
        }
        triggerNotification(notifyReasonId, "تحديث حجز #${bookingId.take(6)} إلى: $newStatus", null, "user")
        logSystemAction(actorName, "تعديل حالة حجز #${bookingId.take(6)} إلى $newStatus", "Updated booking #${bookingId.take(6)} to $newStatus")
    }

    fun assignBookingToTechnician(bookingId: String, techId: String, actorName: String) {
        firestore.collection("bookings").document(bookingId).update(
            "assignedProviderId", techId,
            "status", "تم القبول"
        )
        triggerNotification("2", "تم توجيه حجز جديد لك بانتظار تواصلك الفوري", null, "provider")
        logSystemAction(actorName, "تحويل حجز #${bookingId.take(6)} للفني $techId", "Assigned booking #${bookingId.take(6)} to tech $techId")
    }

    fun deleteBooking(bookingId: String, actorName: String) {
        firestore.collection("bookings").document(bookingId).delete()
        logSystemAction(actorName, "حذف حجز رقم #${bookingId.take(6)}", "Deleted booking #${bookingId.take(6)}")
    }

    // --- Manage Custom Fields ---
    fun addBookingField(labelAr: String, labelEn: String, type: String, isRequired: Boolean, options: List<String>) {
        val fId = UUID.randomUUID().toString()
        val order = (_bookingFields.value.maxOfOrNull { it.displayOrder } ?: 0) + 1
        val item = FormFieldEntity(fId, labelAr, labelEn, type, isRequired, options, order)
        firestore.collection("booking_fields").document(fId).set(item.toMap())
        logSystemAction("المالك", "إضافة حقل مخصص في استمارة الحجز: $labelAr", "Added custom form input field: $labelEn")
    }

    fun editBookingField(field: FormFieldEntity) {
        firestore.collection("booking_fields").document(field.id).set(field.toMap())
        logSystemAction("المالك", "تعديل حقل الاستمارة: ${field.labelAr}", "Edited custom form input field: ${field.labelEn}")
    }

    fun deleteBookingField(id: String) {
        firestore.collection("booking_fields").document(id).delete()
        logSystemAction("المالك", "حذف حقل الاستمارة: $id", "Deleted custom form input field: $id")
    }

    // --- Supervisor Administration ---
    fun addSupervisor(username: String, passcode: String, catId: String) {
        val sId = UUID.randomUUID().toString()
        val sup = AdminSupervisorEntity(sId, username, passcode, "supervisor", catId)
        firestore.collection("admin_supervisors").document(sId).set(sup.toMap())
        logSystemAction("المالك", "تثبيت تعيين مشرف قسم: $username للقسم لقاء الكود $catId", "Assigned category supervisor $username for category $catId")
    }

    fun deleteSupervisor(id: String) {
        firestore.collection("admin_supervisors").document(id).delete()
        logSystemAction("المالك", "حذف مشرف قسم رقم $id", "Removed supervisor account ID $id")
    }

    // --- System Audit Actions ---
    fun logSystemAction(actor: String, actionAr: String, actionEn: String) {
        val logId = UUID.randomUUID().toString()
        val log = ActivityLogEntity(
            id = logId,
            actor = actor,
            actionAr = actionAr,
            actionEn = actionEn,
            timestamp = System.currentTimeMillis()
        )
        firestore.collection("activity_logs").document(logId).set(log.toMap())
    }

    // --- Notification Rules ---
    fun triggerNotification(reasonId: String, detailsText: String, imageUrl: String? = null, targetRecipient: String = "all") {
        val uuid = UUID.randomUUID().toString()
        val config = _notificationConfigs.value.find { it.id == reasonId }
        val isEnabled = config?.isEnabled ?: true
        if (!isEnabled) return

        val title = config?.titleAr ?: "دليل خدمات يمن السعيد 🔔"
        val body = "${config?.bodyTemplateAr ?: "تحديث جديد:"} $detailsText"

        val record = NotificationLogEntity(
            id = uuid,
            title = title,
            body = body,
            imageOrLink = imageUrl,
            recipient = targetRecipient,
            timestamp = System.currentTimeMillis()
        )
        firestore.collection("notifications_log").document(uuid).set(record.toMap())
    }

    fun toggleNotificationConfig(id: String, enabled: Boolean) {
        firestore.collection("notifications_config").document(id).update("isEnabled", enabled)
    }

    fun updateNotificationText(id: String, titleAr: String, titleEn: String, templateAr: String, templateEn: String) {
        firestore.collection("notifications_config").document(id).update(
            "titleAr", titleAr,
            "titleEn", titleEn,
            "bodyTemplateAr", templateAr,
            "bodyTemplateEn", templateEn
        )
    }

    fun sendManualNotification(title: String, body: String, recipient: String, imageUrl: String? = null) {
        val uuid = UUID.randomUUID().toString()
        val record = NotificationLogEntity(
            id = uuid,
            title = title,
            body = body,
            imageOrLink = imageUrl,
            recipient = recipient,
            timestamp = System.currentTimeMillis()
        )
        firestore.collection("notifications_log").document(uuid).set(record.toMap())
    }

    // --- Coupons & Ads Operations ---
    fun addCoupon(code: String, percent: Int) {
        val coupon = CouponEntity(code, percent, true)
        firestore.collection("coupons").document(code).set(coupon.toMap())
    }

    fun deleteCoupon(code: String) {
        firestore.collection("coupons").document(code).delete()
    }

    fun addAdCampaign(title: String, desc: String, price: Double, days: Int) {
        val id = UUID.randomUUID().toString()
        val camp = AdCampaignEntity(id, title, desc, "", price, days)
        firestore.collection("ad_campaigns").document(id).set(camp.toMap())
    }

    fun deleteAdCampaign(id: String) {
        firestore.collection("ad_campaigns").document(id).delete()
    }
}
