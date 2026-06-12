package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.*
import com.example.data.repository.AppRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
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
        activeProviders,
        _searchQuery,
        _selectedCategoryId,
        _selectedDistrict,
        _minRating
    ) { list, query, catId, district, rating ->
        list.filter { p ->
            val matchesQuery = query.isEmpty() || p.fullName.contains(query, ignoreCase = true) || p.phone.contains(query) || p.address.contains(query, ignoreCase = true)
            val matchesCategory = catId == null || p.mainCategoryId == catId || p.subCategoryId == catId
            val matchesDistrict = district == null || p.district.contains(district, ignoreCase = true) || p.address.contains(district, ignoreCase = true)
            val matchesRating = p.averageRating >= rating
            matchesQuery && matchesCategory && matchesDistrict && matchesRating
        }
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
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            repository.submitProviderRegistration(
                fullName = fullName,
                phone = phone,
                mainCatId = mainCatId,
                subCatId = subCatId,
                address = address,
                district = district,
                lat = 15.3186,
                lng = 44.2045,
                profileImageUri = "",
                idCardImageUri = ""
            )
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
