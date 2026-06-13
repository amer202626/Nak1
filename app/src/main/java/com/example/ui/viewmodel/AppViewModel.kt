package com.example.ui.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.*
import com.example.data.repository.AppRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AppRepository(application)

    // --- Screen State Navigation ---
    var currentScreen by mutableStateOf("home") // "home", "providers_list", "provider_details", "register_provider", "admin_panel", "gemini_chat", "user_chat"
    var selectedCategory by mutableStateOf<CategoryEntity?>(null)
    var selectedProvider by mutableStateOf<ProviderEntity?>(null)
    var selectedChatId by mutableStateOf<String?>(null)
    var selectedChatParticipantName by mutableStateOf("")

    // -- Exposed Flows --
    val categories = repository.categories.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val allProviders = repository.allProviders.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val activeProviders = repository.activeProviders.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val recommendedProviders = repository.recommendedProviders.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val pendingProviders = repository.pendingProviders.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val pendingCount = repository.pendingCount.stateIn(viewModelScope, SharingStarted.Eagerly, 0)
    val appSettings = repository.appSettings.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val activeBanners = repository.activeBanners.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val reports = repository.reports.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val reviews = repository.reviews.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val activeTerms = repository.activeTerms.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val chats = repository.chats.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val activityLogs = repository.activityLogs.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // --- Search & Filters ---
    var searchQuery by mutableStateOf("")
    var filterDistrict by mutableStateOf("")
    var filterOnlyVerified by mutableStateOf(false)
    var filterOnlyVip by mutableStateOf(false)

    // --- Gemini Virtual Assistant Chat State ---
    private val _geminiMessages = MutableStateFlow<List<Pair<String, Boolean>>>(listOf(
        Pair("مرحباً بك يا غالي! أنا مساعد WAM الذكي لدليل خدمات اليمن الفاخر. كيف أقدر أخدمك اليوم؟", false)
    ))
    val geminiMessages: StateFlow<List<Pair<String, Boolean>>> = _geminiMessages.asStateFlow()
    var geminiIsLoading by mutableStateOf(false)

    // --- Local chats messages state ---
    private val _currentChatMessages = MutableStateFlow<List<MessageEntity>>(emptyList())
    val currentChatMessages: StateFlow<List<MessageEntity>> = _currentChatMessages.asStateFlow()

    // --- UI Controls helper connection status ---
    fun isOnline(): Boolean = repository.isConnected()

    // --- User Chat action ---
    fun selectChat(chatId: String, participantName: String) {
        selectedChatId = chatId
        selectedChatParticipantName = participantName
        currentScreen = "user_chat"
        viewModelScope.launch {
            repository.getMessagesForChat(chatId).collect {
                _currentChatMessages.value = it
            }
        }
    }

    fun sendLocalMessage(text: String) {
        val chatId = selectedChatId ?: return
        val otherParticipant = selectedChatParticipantName
        viewModelScope.launch {
            repository.sendMessage(chatId, "USER", otherParticipant, text)
        }
    }

    fun startDirectChatWithProvider(provider: ProviderEntity) {
        val chatId = "chat_user_${provider.id}"
        selectChat(chatId, provider.fullName)
    }

    // --- Virtual assistant dialog action ---
    fun sendGeminiPrompt(prompt: String) {
        if (prompt.trim().isEmpty()) return
        
        // Append user prompt
        _geminiMessages.update { it + Pair(prompt, true) }
        geminiIsLoading = true

        viewModelScope.launch {
            try {
                val reply = repository.getGeminiResponse(prompt)
                _geminiMessages.update { it + Pair(reply, false) }
            } catch (e: Exception) {
                _geminiMessages.update { it + Pair("عذراً، حدث خطأ أثناء معالجة الأمر الفوري: ${e.localizedMessage}", false) }
            } finally {
                geminiIsLoading = false
            }
        }
    }

    fun clearVirtualAssistantChat() {
        _geminiMessages.value = listOf(
            Pair("مرحباً بك يا غالي! أنا مساعد WAM الذكي لدليل خدمات اليمن الفاخر. كيف أقدر أخدمك اليوم؟", false)
        )
    }

    // --- Settings, CSV, backup controllers ---
    fun saveSettings(settings: AppSettingEntity) {
        viewModelScope.launch { repository.saveSettings(settings) }
    }

    fun addCategory(id: String, nameAr: String, nameEn: String, imageUrl: String, order: Int) {
        viewModelScope.launch {
            repository.insertCategory(CategoryEntity(id, nameAr, nameEn, imageUrl, null, order, true))
        }
    }

    fun deleteCategory(id: String) {
        viewModelScope.launch { repository.deleteCategory(id) }
    }

    // --- Provider Actions ---
    fun registerProvider(
        fullName: String, phone: String, mainCat: String, subCat: String,
        address: String, district: String, lat: Double, lng: Double,
        profileImage: String, cardImage: String, workImages: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            repository.submitProviderRegistration(
                fullName, phone, mainCat, subCat, address, district,
                lat, lng, profileImage, cardImage, workImages
            )
            onSuccess()
        }
    }

    fun acceptProvider(pendingId: String) {
        viewModelScope.launch { repository.acceptProviderRegistration(pendingId) }
    }

    fun rejectProvider(pendingId: String, reason: String) {
        viewModelScope.launch { repository.rejectProviderRegistration(pendingId, reason) }
    }

    fun addProviderDirectly(fullName: String, phone: String, mainCat: String, address: String, district: String, isVip: Boolean) {
        viewModelScope.launch {
            repository.addProviderDirectly(fullName, phone, mainCat, "", address, district, isVip)
        }
    }

    fun deleteProvider(id: String) {
        viewModelScope.launch { repository.deleteProvider(id) }
    }

    fun updateProviderFeatures(id: String, isVer: Boolean, isPin: Boolean, isRec: Boolean, isSub: Boolean) {
        viewModelScope.launch {
            repository.updateProviderFeatures(id, isVer, isPin, isRec, isSub)
        }
    }

    fun updateProviderDetails(
        id: String, fullName: String, phone: String, address: String,
        district: String, profileImageUrl: String, workImagesCSV: String
    ) {
        viewModelScope.launch {
            repository.updateProviderDetails(id, fullName, phone, address, district, profileImageUrl, workImagesCSV)
        }
    }

    // --- Submitting report and reviews ---
    fun getReviewsForProvider(providerId: String): Flow<List<ReviewEntity>> {
        return repository.getReviewsForProvider(providerId)
    }

    fun submitReview(providerId: String, rating: Int, comment: String) {
        viewModelScope.launch {
            repository.submitReview(providerId, "USER_ME", rating, comment)
        }
    }

    fun submitReport(providerId: String, reason: String, details: String) {
        viewModelScope.launch {
            repository.submitReport(providerId, "USER_ME", reason, details)
        }
    }

    fun deleteReview(reviewId: String) {
        viewModelScope.launch { repository.deleteReview(reviewId) }
    }

    fun deleteReport(id: String) {
        viewModelScope.launch { repository.deleteReport(id) }
    }

    fun addBanner(title: String, type: String, redirect: String, size: String, duration: Int) {
        viewModelScope.launch { repository.addBanner(title, type, redirect, size, duration) }
    }

    fun deleteBanner(id: String) {
        viewModelScope.launch { repository.deleteBanner(id) }
    }

    fun addTerm(termText: String, order: Int) {
        viewModelScope.launch { repository.addRegistrationTerm(termText, order) }
    }

    fun deleteTerm(id: String) {
        viewModelScope.launch { repository.deleteRegistrationTerm(id) }
    }

    fun clearChats() {
        viewModelScope.launch { repository.clearAllChatHistory() }
    }

    // --- CSV helper and backups exports ---
    suspend fun getTableCSV(table: String): String {
        return repository.exportDatabaseToCSV(table)
    }

    fun exportBackup(onExported: (String) -> Unit) {
        viewModelScope.launch {
            val cacheFolder = applicationContext().cacheDir.absolutePath
            val file = repository.backupDatabaseToFile(cacheFolder)
            onExported("تم تصدير النسخة بنجاح للمسار:\n${file.absolutePath}")
        }
    }

    fun importBackup(path: String, onCompleted: (Boolean) -> Unit) {
        viewModelScope.launch {
            val file = java.io.File(path)
            if (file.exists()) {
                val res = repository.restoreDatabaseFromFile(file)
                onCompleted(res)
            } else {
                onCompleted(false)
            }
        }
    }

    private fun applicationContext() = getApplication<Application>().applicationContext
}
