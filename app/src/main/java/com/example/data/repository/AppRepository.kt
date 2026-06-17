package com.example.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.example.BuildConfig
import com.example.data.database.AppDao
import com.example.data.database.AppDatabase
import com.example.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
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

class AppRepository(private val context: Context) {
    private val appDatabase = AppDatabase.getDatabase(context)
    private val dao: AppDao = appDatabase.appDao()

    // --- OkHttpClient for Gemini API ---
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    // --- Flows ---
    val categories: Flow<List<CategoryEntity>> = dao.getAllCategoriesFlow()
    val activeProviders: Flow<List<ProviderEntity>> = dao.getAllActiveProvidersFlow()
    val allProviders: Flow<List<ProviderEntity>> = dao.getAllProvidersFlow()
    val recommendedProviders: Flow<List<ProviderEntity>> = dao.getRecommendedProvidersFlow()
    val pendingProviders: Flow<List<PendingProviderEntity>> = dao.getAllPendingProvidersFlow()
    val pendingCount: Flow<Int> = dao.getPendingCountFlow()
    val appSettings: Flow<AppSettingEntity?> = dao.getSettingsFlow()
    val activeBanners: Flow<List<BannerEntity>> = dao.getActiveBannersFlow()
    val allBanners: Flow<List<BannerEntity>> = dao.getAllBannersFlow()
    val reports: Flow<List<ReportEntity>> = dao.getAllReportsFlow()
    val reviews: Flow<List<ReviewEntity>> = dao.getAllReviewsFlow()
    val chats: Flow<List<ChatEntity>> = dao.getAllChatsFlow()
    val activityLogs: Flow<List<ActivityLogEntity>> = dao.getAllActivityLogsFlow()
    val activeTerms: Flow<List<RegistrationTermEntity>> = dao.getActiveRegistrationTermsFlow()
    val allTerms: Flow<List<RegistrationTermEntity>> = dao.getAllRegistrationTermsFlow()
    val allBookings: Flow<List<BookingEntity>> = dao.getAllBookingsFlow()
    val allNotifications: Flow<List<NotificationEntity>> = dao.getAllNotificationsFlow()

    fun getMessagesForChat(chatId: String): Flow<List<MessageEntity>> = dao.getMessagesForChatFlow(chatId)
    fun getReviewsForProvider(providerId: String): Flow<List<ReviewEntity>> = dao.getReviewsForProviderFlow(providerId)

    // --- Methods for Admins and Settings ---
    suspend fun saveSettings(settings: AppSettingEntity) = withContext(Dispatchers.IO) {
        dao.insertSettings(settings)
        val sharedPrefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        sharedPrefs.edit().apply {
            putString("primary_color", settings.primaryColor)
            putString("secondary_color", settings.secondaryColor)
            putString("support_phone", settings.supportPhone)
            putString("support_whatsapp", settings.supportWhatsApp)
            putString("footer_text", settings.footerText)
            apply()
        }
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
        dao.insertCategory(category)
        logAction("ADMIN", "إضافة قسم", "تم إضافة أو تعديل القسم: ${category.nameAr}")
    }

    suspend fun deleteCategory(id: String) = withContext(Dispatchers.IO) {
        dao.deleteCategoryById(id)
        logAction("ADMIN", "حذف قسم", "تم حذف معرف القسم $id")
    }

    // --- Registration Forms (User and Professionally Registering) ---
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
        dao.insertPendingProvider(newPending)
        logAction("USER", "تسجيل مهني جديد", "قدم المهني $fullName طلباً للتطبيق")
    }

    // --- Admin Decision actions ---
    suspend fun acceptProviderRegistration(pendingId: String) = withContext(Dispatchers.IO) {
        val list = pendingProviders.firstOrNull() ?: emptyList()
        val pending = list.find { it.id == pendingId }
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
            dao.insertProvider(provider)
            dao.deletePendingProviderById(pendingId)
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
        val list = allProviders.firstOrNull() ?: emptyList()
        val target = list.find { it.id == id }
        if (target != null) {
            val updated = target.copy(
                fullName = fullName,
                phone = phone,
                address = address,
                district = district,
                profileImageUrl = profileImageUrl,
                workImagesCSV = workImagesCSV
            )
            dao.updateProvider(updated)
            logAction("ADMIN", "تعديل تفاصيل مهني", "تم تعديل معلومات مقدم الخدمة $fullName من قبل الإدارة")
        }
    }

    suspend fun rejectProviderRegistration(pendingId: String, reason: String) = withContext(Dispatchers.IO) {
        val list = pendingProviders.firstOrNull() ?: emptyList()
        val pending = list.find { it.id == pendingId }
        if (pending != null) {
            val rejected = pending.copy(status = "rejected", rejectReason = reason)
            dao.insertPendingProvider(rejected)
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
        dao.insertProvider(provider)
        logAction("ADMIN", "إضافة فني يدوياً", "تم إضافة الفني ${provider.fullName} مباشرة للدليل")
    }

    suspend fun deleteProvider(id: String) = withContext(Dispatchers.IO) {
        dao.deleteProviderById(id)
        logAction("ADMIN", "حذف فني نشط", "تمت إزالة المهني ذو المعرف $id من الدليل")
    }

    suspend fun updateProviderFeatures(
        id: String,
        isVerified: Boolean,
        isPinned: Boolean,
        isRecommended: Boolean,
        isSubscribed: Boolean
    ) = withContext(Dispatchers.IO) {
        val expiry = if (isSubscribed) System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000) else 0L
        dao.updateProviderFeaturesDirect(id, isVerified, isPinned, isRecommended, isSubscribed, expiry)
        logAction("ADMIN", "تعديل مزايا مهني", "تم تعديل المزايا والموثوقية للمهني ذي المعرف $id")
    }

    // --- Reviews and loyalty points ---
    suspend fun submitReview(providerId: String, userId: String, rating: Int, comment: String) = withContext(Dispatchers.IO) {
        val reviewId = "rev_${UUID.randomUUID()}"
        val newReview = ReviewEntity(reviewId, providerId, userId, rating, comment, System.currentTimeMillis())
        dao.insertReview(newReview)

        // Award 15 loyalty points to user
        val currentPoints = dao.getLoyaltyPointsForUser(userId)
        val pointsToSave = (currentPoints?.points ?: 0) + 15
        dao.insertLoyaltyPoints(LoyaltyPointsEntity(userId, pointsToSave))

        // Re-calculate provider average rating
        val list = allProviders.firstOrNull() ?: emptyList()
        val provider = list.find { it.id == providerId }
        if (provider != null) {
            val rateFlow = dao.getReviewsForProviderFlow(providerId).firstOrNull() ?: emptyList()
            val totalQty = rateFlow.size
            val sumRates = rateFlow.sumOf { it.rating }
            val newAvg = if (totalQty > 0) sumRates.toDouble() / totalQty else rating.toDouble()
            dao.updateProvider(provider.copy(averageRating = newAvg, totalReviews = totalQty))
        }
        logAction("USER", "تقييم مهني", "أعطى المستخدم $userId تقييماً بمقدار $rating للمهني $providerId")
    }

    suspend fun deleteReview(reviewId: String) = withContext(Dispatchers.IO) {
        dao.deleteReviewById(reviewId)
        logAction("ADMIN", "مسح تقييم", "حذف التحكم تقييماً برقم $reviewId")
    }

    // --- Chats & messages ---
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

    // --- Banners ---
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
        dao.insertBanner(b)
        logAction("ADMIN", "زيادة ممول", "تم إضافة إعلان ممول باسم: $title")
    }

    suspend fun deleteBanner(id: String) = withContext(Dispatchers.IO) {
        dao.deleteBannerById(id)
        logAction("ADMIN", "حذف إعلان", "تم مسح البانر الممول ذو الرقم $id")
    }

    // --- Registration Terms ---
    suspend fun addRegistrationTerm(text: String, order: Int) = withContext(Dispatchers.IO) {
        dao.insertRegistrationTerm(
            RegistrationTermEntity(
                id = "term_${UUID.randomUUID()}",
                termText = text,
                order = order,
                isActive = true
            )
        )
    }

    suspend fun deleteRegistrationTerm(id: String) = withContext(Dispatchers.IO) {
        dao.deleteRegistrationTermById(id)
    }

    // --- User Reports ---
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
        dao.insertReport(r)
        logAction("USER", "إرسال بلاغ عن فني", "تم الإبلاغ عن الفني $providerId بسبب $reason")
    }

    suspend fun deleteReport(id: String) = withContext(Dispatchers.IO) {
        dao.deleteReportById(id)
    }

    // --- Offline-first simulated database exports / Backups ---
    suspend fun exportDatabaseToCSV(tableName: String): String = withContext(Dispatchers.IO) {
        val sb = java.lang.StringBuilder()
        when (tableName) {
            "providers" -> {
                sb.append("id,fullName,phone,mainCategory,address,rating,isSubscribed\n")
                val list = allProviders.firstOrNull() ?: emptyList()
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
        val appSettingsObj = dao.getSettingsDirect()
        val providersArray = JSONArray()
        dao.getAllProvidersFlow().firstOrNull()?.forEach {
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
            
            val currentSettings = dao.getSettingsDirect() ?: AppSettingEntity(
                appName = appName,
                welcomeMessage = "أهلاً ومرحباً بكم مع تطبيق كل خدمات اليمن - دليل أصحاب المهن الفاخر",
                footerText = footerText,
                supportPhone = "777644670",
                supportEmail = "support@wam2026.com",
                supportWhatsApp = "+967777644670",
                primaryColor = "#ECEFF1",
                secondaryColor = "#37474F",
                fontFamily = "Cairo",
                fontSize = 14,
                chatEnabled = true,
                assistantEnabled = true,
                radiusSearchLimit = 10,
                voiceSearchEnabled = true,
                maintenanceMode = false,
                maintenanceMessage = ""
            )
            dao.insertSettings(currentSettings.copy(appName = appName, footerText = footerText))
            
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
                    dao.insertProvider(p)
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
        val settings = dao.getSettingsDirect()
        val isOnline = isConnected()
        val supportNum = if (isOnline) (settings?.supportPhone ?: "777644670") else "wam777644"

        // Local Offline Q&A Index Answers for fallbacks or offline requests
        val promptClean = userPrompt.trim()
        val isOffline = !isOnline
        
        if (isOffline) {
            return@withContext getOfflineResponse(promptClean, supportNum)
        }

        // Online Gemini Request (Option B - Direct REST API)
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            // No API Key or placeholder inside config -> Return Offline answers gracefully
            return@withContext getOfflineResponse(promptClean, supportNum) + " (وضع دون اتصال - لا يوجد مفتاح Gemini معرّف)"
        }

        // REST endpoint using gemini-3.5-flash as default as instructed by gemini-api skill
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
        
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

    suspend fun insertBooking(booking: BookingEntity) = withContext(Dispatchers.IO) {
        dao.insertBooking(booking)
        logAction("USER", "حجز جديد", "تم تقديم طلب حجز لمزود الخدمة ${booking.providerName}")
    }

    suspend fun updateBookingStatus(id: String, status: String) = withContext(Dispatchers.IO) {
        dao.updateBookingStatus(id, status)
        logAction("ADMIN", "تعديل حالة حجز", "تم تعديل حالة الحجز $id إلى $status")
    }

    suspend fun deleteBooking(id: String) = withContext(Dispatchers.IO) {
        dao.deleteBookingById(id)
        logAction("ADMIN", "حذف حجز", "تم حذف طلب الحجز ذي المعرف $id")
    }

    suspend fun insertNotification(notification: NotificationEntity) = withContext(Dispatchers.IO) {
        dao.insertNotification(notification)
        logAction("ADMIN", "إرسال إشعار", "تم إرسال إشعار جديد بعنوان: ${notification.title}")
    }

    suspend fun deleteNotification(id: String) = withContext(Dispatchers.IO) {
        dao.deleteNotificationById(id)
        logAction("ADMIN", "حذف إشعار", "تم حذف الإشعار ذي المعرف $id")
    }

    suspend fun clearAllNotifications() = withContext(Dispatchers.IO) {
        dao.deleteAllNotifications()
        logAction("ADMIN", "مسح الإشعارات", "تم مسح كافة الإشعارات الفورية")
    }
}
