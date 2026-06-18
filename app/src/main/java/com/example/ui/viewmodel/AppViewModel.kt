package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.*
import com.example.data.repository.AppRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AppRepository(application)
    
    // SharedPreferences for API Key and Theme
    private val prefs = application.getSharedPreferences("WAM_PREFS", Context.MODE_PRIVATE)

    val categories: StateFlow<List<CategoryEntity>> = repository.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val providers: StateFlow<List<ProviderEntity>> = repository.allProviders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bookings: StateFlow<List<BookingEntity>> = repository.allBookings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notifications: StateFlow<List<NotificationEntity>> = repository.allNotifications
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val chats: StateFlow<List<ChatEntity>> = repository.allChats
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedChatId = MutableStateFlow<String?>(null)
    val selectedChatId: StateFlow<String?> = _selectedChatId.asStateFlow()

    private val _aiResponse = MutableStateFlow("")
    val aiResponse: StateFlow<String> = _aiResponse.asStateFlow()

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    private val _language = MutableStateFlow(prefs.getString("lang", "ar") ?: "ar")
    val language: StateFlow<String> = _language.asStateFlow()

    private val _geminiApiKey = MutableStateFlow(prefs.getString("gemini_key", "") ?: "")
    val geminiApiKey: StateFlow<String> = _geminiApiKey.asStateFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val activeChatMessages: StateFlow<List<ChatMessageEntity>> = _selectedChatId
        .flatMapLatest { chatId ->
            if (chatId != null) repository.getMessagesForChat(chatId)
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            repository.populateStarterDataIfNeeded()
            // Start real-time background background sync loop polling Firestore every 6 seconds!
            while (true) {
                try {
                    repository.syncAllFromFirestore()
                } catch (e: Exception) {
                    android.util.Log.e("AppViewModel", "Sync loop error", e)
                }
                kotlinx.coroutines.delay(6000) // Poll Firestore every 6 seconds
            }
        }
    }

    fun getFirestoreProjectId(): String {
        return repository.getFirestoreProjectId()
    }

    fun setFirestoreProjectId(id: String) {
        repository.setFirestoreProjectId(id)
        viewModelScope.launch {
            try {
                repository.syncAllFromFirestore()
            } catch (e: Exception) {
                android.util.Log.e("AppViewModel", "Manual sync error", e)
            }
        }
    }

    fun setLanguage(lang: String) {
        _language.value = lang
        prefs.edit().putString("lang", lang).apply()
    }

    fun setGeminiApiKey(key: String) {
        _geminiApiKey.value = key
        prefs.edit().putString("gemini_key", key).apply()
    }

    // Bookings logic
    fun submitBooking(providerId: String, providerName: String, fullName: String, phone: String, district: String) {
        viewModelScope.launch {
            val booking = BookingEntity(
                id = UUID.randomUUID().toString(),
                providerId = providerId,
                providerName = providerName,
                fullName = fullName,
                phone = phone,
                district = district,
                status = "pending",
                createdAt = System.currentTimeMillis()
            )
            repository.insertBooking(booking)
            
            // Generate auto notification for booking request
            val notif = NotificationEntity(
                id = UUID.randomUUID().toString(),
                title = "طلب حجز جديد 📅",
                body = "لقد أرسلت طلب حجز للفني $providerName وسيتم مراجعته وتأكيده قريباً.",
                type = "booking",
                targetId = providerId,
                createdAt = System.currentTimeMillis()
            )
            repository.insertNotification(notif)
        }
    }

    fun updateBookingStatus(id: String, status: String) {
        viewModelScope.launch {
            repository.updateBookingStatus(id, status)
            
            // Add custom notification for status update
            val statusAr = when(status) {
                "confirmed" -> "مؤكد ومقبول 🟢"
                "completed" -> "تم الإنجاز بنجاح 🔵"
                "cancelled" -> "ملغى 🔴"
                else -> status
            }
            val notif = NotificationEntity(
                id = UUID.randomUUID().toString(),
                title = "تحديث حالة الحجز 📅",
                body = "تم تحديث حالة طلب الحجز الخاص بك لتصبح: $statusAr",
                type = "booking_update",
                targetId = "",
                createdAt = System.currentTimeMillis()
            )
            repository.insertNotification(notif)
        }
    }

    fun deleteBooking(id: String) {
        viewModelScope.launch {
            repository.deleteBooking(id)
        }
    }

    // Notifications logic
    fun sendSystemNotification(title: String, body: String, type: String, targetId: String) {
        viewModelScope.launch {
            val notification = NotificationEntity(
                id = UUID.randomUUID().toString(),
                title = title,
                body = body,
                type = type,
                targetId = targetId,
                createdAt = System.currentTimeMillis()
            )
            repository.insertNotification(notification)
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

    // Chats logic
    fun selectActiveChat(chatId: String?) {
        _selectedChatId.value = chatId
    }

    fun openChatWithProvider(providerId: String, providerName: String) {
        viewModelScope.launch {
            val chatId = repository.createChatRoom(providerId, providerName)
            _selectedChatId.value = chatId
        }
    }

    fun sendChatMessage(text: String, senderId: String, senderName: String) {
        val chatId = _selectedChatId.value ?: return
        viewModelScope.launch {
            repository.sendMessage(chatId, text, senderId, senderName)
        }
    }

    // AI Assistant Consulting
    fun askGeminiAssistant(prompt: String) {
        if (prompt.isBlank()) return
        _isAiLoading.value = true
        _aiResponse.value = ""
        viewModelScope.launch {
            val response = repository.askGemini(prompt, _geminiApiKey.value)
            _aiResponse.value = response
            _isAiLoading.value = false
            
            // Insert AI system message into notification for user reference
            val notif = NotificationEntity(
                id = UUID.randomUUID().toString(),
                title = "رد استشاري الذكاء الاصطناعي 🤖",
                body = if (response.length > 100) response.take(100) + "..." else response,
                type = "ai",
                targetId = "",
                createdAt = System.currentTimeMillis()
            )
            repository.insertNotification(notif)
        }
    }

    // Register / Add provider
    fun registerProvider(
        name: String,
        categoryId: String,
        phone: String,
        district: String,
        details: String,
        imageBase64: String
    ) {
        viewModelScope.launch {
            val provider = ProviderEntity(
                id = UUID.randomUUID().toString(),
                fullName = name,
                providerType = categoryId,
                phone = phone,
                district = district,
                details = details,
                imageBase64 = imageBase64,
                status = "approved", // Approved instantly to let the user see it in the directory list on creation
                rate = 5.0,
                reviewsCount = 1,
                isVerified = true, // Highlight with verified gold star accent instantly
                dateAdded = System.currentTimeMillis()
            )
            repository.insertProvider(provider)
            
            // Add notification for admin review
            val notif = NotificationEntity(
                id = UUID.randomUUID().toString(),
                title = "تم تفعيل ومزامنة الشريك الفني الجديد 🎉",
                body = "تم إدراج ومزامنة الشريك الفني $name بنجاح في قسم $categoryId وتحويل حالته لنشط معتمد فورا.",
                type = "provider_registration",
                targetId = "",
                createdAt = System.currentTimeMillis()
            )
            repository.insertNotification(notif)
        }
    }

    // Admin direct additions/approvals
    fun approveProvider(id: String) {
        viewModelScope.launch {
            val list = providers.value
            val found = list.find { it.id == id }
            if (found != null) {
                val updated = found.copy(status = "approved")
                repository.insertProvider(updated) // REPLACE conflict inserts updated
                
                // Add notification
                val notif = NotificationEntity(
                    id = UUID.randomUUID().toString(),
                    title = "موافقة على فني جديد 🎉",
                    body = "تم قبول انضمام الفني ${found.fullName} في منصة WAM Services بنجاح وتحويل حالته لنشط.",
                    type = "provider_approved",
                    targetId = "",
                    createdAt = System.currentTimeMillis()
                )
                repository.insertNotification(notif)
            }
        }
    }

    fun deleteProvider(id: String) {
        viewModelScope.launch {
            repository.deleteProvider(id)
        }
    }

    fun addNewCategory(id: String, nameAr: String, nameEn: String, emoji: String) {
        viewModelScope.launch {
            val cat = CategoryEntity(id, nameAr, nameEn, emoji, priority = 0)
            repository.insertCategory(cat)
        }
    }

    fun deleteCategory(category: CategoryEntity) {
        viewModelScope.launch {
            repository.deleteCategory(category)
        }
    }

    fun addAndApproveProvider(
        name: String,
        categoryId: String,
        phone: String,
        district: String,
        details: String
    ) {
        viewModelScope.launch {
            val provider = ProviderEntity(
                id = UUID.randomUUID().toString(),
                fullName = name,
                providerType = categoryId,
                phone = phone,
                district = district,
                details = details,
                status = "approved", // approved instantly
                rate = 5.0,
                reviewsCount = 1,
                isVerified = true,
                dateAdded = System.currentTimeMillis()
            )
            repository.insertProvider(provider)
        }
    }
}
