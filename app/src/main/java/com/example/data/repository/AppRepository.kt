package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.database.AppDatabase
import com.example.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

class AppRepository(private val context: Context) {
    private val db = AppDatabase.getDatabase(context)
    
    val allCategories: Flow<List<CategoryEntity>> = db.categoryDao().getAllCategories()
    val allProviders: Flow<List<ProviderEntity>> = db.providerDao().getAllProviders()
    val allBookings: Flow<List<BookingEntity>> = db.bookingDao().getAllBookings()
    val allNotifications: Flow<List<NotificationEntity>> = db.notificationDao().getAllNotifications()
    val allChats: Flow<List<ChatEntity>> = db.chatDao().getAllChats()

    private val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    
    fun getFirestoreProjectId(): String {
        return prefs.getString("firestore_project_id", "wam-services-dc8d3") ?: "wam-services-dc8d3"
    }
    
    fun setFirestoreProjectId(id: String) {
        prefs.edit().putString("firestore_project_id", id).apply()
    }

    private suspend fun pushToFirestore(collection: String, documentId: String, data: Map<String, Any?>) = withContext(Dispatchers.IO) {
        try {
            val projectId = getFirestoreProjectId()
            val urlString = "https://firestore.googleapis.com/v1/projects/$projectId/databases/(default)/documents/$collection/$documentId"
            val url = URL(urlString)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true
            
            // Note: Since Firestore REST API requires a PATCH method to do safe overwrite/create, check if Document already exists or use PATCH and query.
            // PATCH with standard update requires query parameters or just normal PUT/POST.
            // Actually, PATCH with url `.../documents/collection/documentId` creates or updates a document. Let's use PATCH!
            val patchUrlStr = "https://firestore.googleapis.com/v1/projects/$projectId/databases/(default)/documents/$collection/$documentId"
            val patchUrl = URL(patchUrlStr)
            val patchConn = patchUrl.openConnection() as HttpURLConnection
            patchConn.requestMethod = "PATCH"
            patchConn.setRequestProperty("Content-Type", "application/json")
            patchConn.doOutput = true

            val fieldsJson = JSONObject()
            for ((key, value) in data) {
                if (value == null) continue
                val valObj = JSONObject()
                when (value) {
                    is Boolean -> valObj.put("booleanValue", value)
                    is Double -> valObj.put("doubleValue", value)
                    is Float -> valObj.put("doubleValue", value.toDouble())
                    is Long -> valObj.put("integerValue", value.toString())
                    is Int -> valObj.put("integerValue", value.toString())
                    else -> valObj.put("stringValue", value.toString())
                }
                fieldsJson.put(key, valObj)
            }
            
            val requestPayload = JSONObject()
            requestPayload.put("fields", fieldsJson)
            
            OutputStreamWriter(patchConn.outputStream).use { writer ->
                writer.write(requestPayload.toString())
                writer.flush()
            }
            
            val code = patchConn.responseCode
            if (code != HttpURLConnection.HTTP_OK) {
                Log.e("FirebaseSync", "Error writing to Firestore $collection/$documentId. Code: $code")
            }
            Unit
        } catch (e: Exception) {
            Log.e("FirebaseSync", "Exception in pushToFirestore", e)
            Unit
        }
    }

    private suspend fun deleteFromFirestore(collection: String, documentId: String) = withContext(Dispatchers.IO) {
        try {
            val projectId = getFirestoreProjectId()
            val urlString = "https://firestore.googleapis.com/v1/projects/$projectId/databases/(default)/documents/$collection/$documentId"
            val url = URL(urlString)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "DELETE"
            val code = conn.responseCode
            if (code != HttpURLConnection.HTTP_OK && code != 204) {
                Log.e("FirebaseSync", "Error deleting from Firestore $collection/$documentId. Code: $code")
            }
            Unit
        } catch (e: Exception) {
            Log.e("FirebaseSync", "Exception in deleteFromFirestore", e)
            Unit
        }
    }

    private suspend fun fetchCollectionFromFirestore(collection: String): List<JSONObject>? = withContext(Dispatchers.IO) {
        val documents = mutableListOf<JSONObject>()
        try {
            val projectId = getFirestoreProjectId()
            val urlString = "https://firestore.googleapis.com/v1/projects/$projectId/databases/(default)/documents/$collection?pageSize=300"
            val url = URL(urlString)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            
            val code = conn.responseCode
            if (code == HttpURLConnection.HTTP_OK) {
                BufferedReader(InputStreamReader(conn.inputStream)).use { reader ->
                    val response = StringBuilder()
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        response.append(line)
                    }
                    val jsonResponse = JSONObject(response.toString())
                    if (jsonResponse.has("documents")) {
                        val docsArray = jsonResponse.getJSONArray("documents")
                        for (i in 0 until docsArray.length()) {
                            documents.add(docsArray.getJSONObject(i))
                        }
                    }
                }
                return@withContext documents
            } else if (code == 404) {
                // Firestore returns 404 when the collection is empty/doesn't exist yet
                return@withContext emptyList<JSONObject>()
            } else {
                Log.e("FirebaseSync", "Error fetching Firestore collection $collection. Code: $code")
                return@withContext null
            }
        } catch (e: Exception) {
            Log.e("FirebaseSync", "Exception in fetchCollectionFromFirestore $collection", e)
            return@withContext null
        }
    }

    private fun parseStringField(fields: JSONObject, key: String, default: String = ""): String {
        if (!fields.has(key)) return default
        val obj = fields.getJSONObject(key)
        return obj.optString("stringValue", default)
    }

    private fun parseBooleanField(fields: JSONObject, key: String, default: Boolean = false): Boolean {
        if (!fields.has(key)) return default
        val obj = fields.getJSONObject(key)
        return obj.optBoolean("booleanValue", default)
    }

    private fun parseDoubleField(fields: JSONObject, key: String, default: Double = 0.0): Double {
        if (!fields.has(key)) return default
        val obj = fields.getJSONObject(key)
        if (obj.has("doubleValue")) {
            return obj.optDouble("doubleValue", default)
        } else if (obj.has("integerValue")) {
            return obj.optDouble("integerValue", default)
        }
        return default
    }

    private fun parseIntField(fields: JSONObject, key: String, default: Int = 0): Int {
        if (!fields.has(key)) return default
        val obj = fields.getJSONObject(key)
        if (obj.has("integerValue")) {
            return obj.optString("integerValue", default.toString()).toIntOrNull() ?: default
        }
        return default
    }

    private fun parseLongField(fields: JSONObject, key: String, default: Long = 0L): Long {
        if (!fields.has(key)) return default
        val obj = fields.getJSONObject(key)
        if (obj.has("integerValue")) {
            return obj.optString("integerValue", default.toString()).toLongOrNull() ?: default
        }
        return default
    }

    suspend fun syncAllFromFirestore() {
        try {
            // 1. Sync Categories
            val cats = fetchCollectionFromFirestore("categories")
            if (cats != null) {
                if (cats.isNotEmpty()) {
                    val entities = cats.map { doc ->
                        val fields = doc.getJSONObject("fields")
                        val name = doc.getString("name")
                        val docId = name.substringAfterLast("/")
                        CategoryEntity(
                            id = docId,
                            nameAr = parseStringField(fields, "nameAr"),
                            nameEn = parseStringField(fields, "nameEn"),
                            emoji = parseStringField(fields, "emoji"),
                            priority = parseIntField(fields, "priority")
                        )
                    }
                    db.categoryDao().clearCategories()
                    for (entity in entities) {
                        db.categoryDao().insertCategory(entity)
                    }
                } else {
                    // Seed Firestore from local DB if local has data
                    val localCats = allCategories.first()
                    if (localCats.isNotEmpty()) {
                        for (cat in localCats) {
                            pushToFirestore("categories", cat.id, mapOf(
                                "nameAr" to cat.nameAr,
                                "nameEn" to cat.nameEn,
                                "emoji" to cat.emoji,
                                "priority" to cat.priority
                            ))
                        }
                    }
                }
            }

            // 2. Sync Providers
            val provs = fetchCollectionFromFirestore("providers")
            if (provs != null) {
                if (provs.isNotEmpty()) {
                    val entities = provs.map { doc ->
                        val fields = doc.getJSONObject("fields")
                        val name = doc.getString("name")
                        val docId = name.substringAfterLast("/")
                        ProviderEntity(
                            id = docId,
                            fullName = parseStringField(fields, "fullName"),
                            providerType = parseStringField(fields, "providerType"),
                            phone = parseStringField(fields, "phone"),
                            status = parseStringField(fields, "status", "approved"),
                            district = parseStringField(fields, "district"),
                            details = parseStringField(fields, "details"),
                            rate = parseDoubleField(fields, "rate", 4.5),
                            reviewsCount = parseIntField(fields, "reviewsCount", 1),
                            imageBase64 = parseStringField(fields, "imageBase64"),
                            isVerified = parseBooleanField(fields, "isVerified"),
                            dateAdded = parseLongField(fields, "dateAdded", System.currentTimeMillis())
                        )
                    }
                    db.providerDao().clearProviders()
                    for (entity in entities) {
                        db.providerDao().insertProvider(entity)
                    }
                } else {
                    // Seed Firestore from local DB
                    val localProvs = allProviders.first()
                    if (localProvs.isNotEmpty()) {
                        for (prov in localProvs) {
                            pushToFirestore("providers", prov.id, mapOf(
                                "fullName" to prov.fullName,
                                "providerType" to prov.providerType,
                                "phone" to prov.phone,
                                "status" to prov.status,
                                "district" to prov.district,
                                "details" to prov.details,
                                "rate" to prov.rate,
                                "reviewsCount" to prov.reviewsCount,
                                "imageBase64" to prov.imageBase64,
                                "isVerified" to prov.isVerified,
                                "dateAdded" to prov.dateAdded
                            ))
                        }
                    }
                }
            }

            // 3. Sync Bookings
            val bks = fetchCollectionFromFirestore("bookings")
            if (bks != null) {
                if (bks.isNotEmpty()) {
                    val entities = bks.map { doc ->
                        val fields = doc.getJSONObject("fields")
                        val name = doc.getString("name")
                        val docId = name.substringAfterLast("/")
                        BookingEntity(
                            id = docId,
                            providerId = parseStringField(fields, "providerId"),
                            providerName = parseStringField(fields, "providerName"),
                            fullName = parseStringField(fields, "fullName"),
                            phone = parseStringField(fields, "phone"),
                            district = parseStringField(fields, "district"),
                            status = parseStringField(fields, "status", "pending"),
                            createdAt = parseLongField(fields, "createdAt", System.currentTimeMillis())
                        )
                    }
                    db.bookingDao().clearBookings()
                    for (entity in entities) {
                        db.bookingDao().insertBooking(entity)
                    }
                } else {
                    val localBookings = allBookings.first()
                    if (localBookings.isNotEmpty()) {
                        for (bk in localBookings) {
                            pushToFirestore("bookings", bk.id, mapOf(
                                "providerId" to bk.providerId,
                                "providerName" to bk.providerName,
                                "fullName" to bk.fullName,
                                "phone" to bk.phone,
                                "district" to bk.district,
                                "status" to bk.status,
                                "createdAt" to bk.createdAt
                            ))
                        }
                    }
                }
            }

            // 4. Sync Notifications
            val notifs = fetchCollectionFromFirestore("notifications")
            if (notifs != null) {
                if (notifs.isNotEmpty()) {
                    val entities = notifs.map { doc ->
                        val fields = doc.getJSONObject("fields")
                        val name = doc.getString("name")
                        val docId = name.substringAfterLast("/")
                        NotificationEntity(
                            id = docId,
                            title = parseStringField(fields, "title"),
                            body = parseStringField(fields, "body"),
                            type = parseStringField(fields, "type", "general"),
                            targetId = parseStringField(fields, "targetId"),
                            createdAt = parseLongField(fields, "createdAt", System.currentTimeMillis())
                        )
                    }
                    db.notificationDao().clearAllNotifications()
                    for (entity in entities) {
                        db.notificationDao().insertNotification(entity)
                    }
                } else {
                    val localNotifs = allNotifications.first()
                    if (localNotifs.isNotEmpty()) {
                        for (notif in localNotifs) {
                            pushToFirestore("notifications", notif.id, mapOf(
                                "title" to notif.title,
                                "body" to notif.body,
                                "type" to notif.type,
                                "targetId" to notif.targetId,
                                "createdAt" to notif.createdAt
                            ))
                        }
                    }
                }
            }

            // 5. Sync Chats
            val chtList = fetchCollectionFromFirestore("chats")
            if (chtList != null) {
                if (chtList.isNotEmpty()) {
                    val entities = chtList.map { doc ->
                        val fields = doc.getJSONObject("fields")
                        val name = doc.getString("name")
                        val docId = name.substringAfterLast("/")
                        ChatEntity(
                            chatId = docId,
                            participantsCSV = parseStringField(fields, "participantsCSV"),
                            lastMessage = parseStringField(fields, "lastMessage"),
                            lastUpdated = parseLongField(fields, "lastUpdated")
                        )
                    }
                    db.chatDao().clearChats()
                    for (entity in entities) {
                        db.chatDao().insertChat(entity)
                    }
                } else {
                    val localChats = allChats.first()
                    if (localChats.isNotEmpty()) {
                        for (ch in localChats) {
                            pushToFirestore("chats", ch.chatId, mapOf(
                                "participantsCSV" to ch.participantsCSV,
                                "lastMessage" to ch.lastMessage,
                                "lastUpdated" to ch.lastUpdated
                            ))
                        }
                    }
                }
            }

            // 6. Sync Chat Messages
            val msgs = fetchCollectionFromFirestore("chat_messages")
            if (msgs != null) {
                if (msgs.isNotEmpty()) {
                    val entities = msgs.map { doc ->
                        val fields = doc.getJSONObject("fields")
                        val name = doc.getString("name")
                        val docId = name.substringAfterLast("/")
                        ChatMessageEntity(
                            id = docId,
                            chatId = parseStringField(fields, "chatId"),
                            senderId = parseStringField(fields, "senderId"),
                            senderName = parseStringField(fields, "senderName"),
                            text = parseStringField(fields, "text"),
                            timestamp = parseLongField(fields, "timestamp")
                        )
                    }
                    db.chatDao().clearChatMessages()
                    for (entity in entities) {
                        db.chatDao().insertMessage(entity)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("FirebaseSync", "syncAllFromFirestore failed", e)
        }
    }

    suspend fun insertCategory(category: CategoryEntity) {
        db.categoryDao().insertCategory(category)
        pushToFirestore("categories", category.id, mapOf(
            "nameAr" to category.nameAr,
            "nameEn" to category.nameEn,
            "emoji" to category.emoji,
            "priority" to category.priority
        ))
    }

    suspend fun deleteCategory(category: CategoryEntity) {
        db.categoryDao().deleteCategory(category)
        deleteFromFirestore("categories", category.id)
    }

    suspend fun insertProvider(provider: ProviderEntity) {
        db.providerDao().insertProvider(provider)
        pushToFirestore("providers", provider.id, mapOf(
            "fullName" to provider.fullName,
            "providerType" to provider.providerType,
            "phone" to provider.phone,
            "status" to provider.status,
            "district" to provider.district,
            "details" to provider.details,
            "rate" to provider.rate,
            "reviewsCount" to provider.reviewsCount,
            "imageBase64" to provider.imageBase64,
            "isVerified" to provider.isVerified,
            "dateAdded" to provider.dateAdded
        ))
    }

    suspend fun updateProvider(provider: ProviderEntity) {
        db.providerDao().updateProvider(provider)
        pushToFirestore("providers", provider.id, mapOf(
            "fullName" to provider.fullName,
            "providerType" to provider.providerType,
            "phone" to provider.phone,
            "status" to provider.status,
            "district" to provider.district,
            "details" to provider.details,
            "rate" to provider.rate,
            "reviewsCount" to provider.reviewsCount,
            "imageBase64" to provider.imageBase64,
            "isVerified" to provider.isVerified,
            "dateAdded" to provider.dateAdded
        ))
    }

    suspend fun deleteProvider(id: String) {
        db.providerDao().deleteProviderById(id)
        deleteFromFirestore("providers", id)
    }

    suspend fun insertBooking(booking: BookingEntity) {
        db.bookingDao().insertBooking(booking)
        pushToFirestore("bookings", booking.id, mapOf(
            "providerId" to booking.providerId,
            "providerName" to booking.providerName,
            "fullName" to booking.fullName,
            "phone" to booking.phone,
            "district" to booking.district,
            "status" to booking.status,
            "createdAt" to booking.createdAt
        ))
    }

    suspend fun updateBookingStatus(id: String, status: String) {
        db.bookingDao().updateBookingStatus(id, status)
        val booking = db.bookingDao().getBookingById(id)
        if (booking != null) {
            pushToFirestore("bookings", id, mapOf(
                "providerId" to booking.providerId,
                "providerName" to booking.providerName,
                "fullName" to booking.fullName,
                "phone" to booking.phone,
                "district" to booking.district,
                "status" to booking.status,
                "createdAt" to booking.createdAt
            ))
        }
    }

    suspend fun deleteBooking(id: String) {
        db.bookingDao().deleteBooking(id)
        deleteFromFirestore("bookings", id)
    }

    suspend fun insertNotification(notification: NotificationEntity) {
        db.notificationDao().insertNotification(notification)
        pushToFirestore("notifications", notification.id, mapOf(
            "title" to notification.title,
            "body" to notification.body,
            "type" to notification.type,
            "targetId" to notification.targetId,
            "createdAt" to notification.createdAt
        ))
    }

    suspend fun deleteNotification(id: String) {
        db.notificationDao().deleteNotification(id)
        deleteFromFirestore("notifications", id)
    }

    suspend fun clearAllNotifications() {
        db.notificationDao().clearAllNotifications()
        // Delete each document or clear them dynamically on Firestore if wanted
    }

    fun getMessagesForChat(chatId: String): Flow<List<ChatMessageEntity>> = db.chatDao().getMessagesForChat(chatId)
    
    suspend fun createChatRoom(providerId: String, providerName: String): String {
        val chatId = "chat_$providerId"
        val existingChats = allChats.first()
        val exists = existingChats.any { it.chatId == chatId }
        if (!exists) {
            val newChat = ChatEntity(
                chatId = chatId,
                participantsCSV = "User, $providerName",
                lastMessage = "مرحباً! كيف يمكنني مساعدتك اليوم؟",
                lastUpdated = System.currentTimeMillis()
            )
            db.chatDao().insertChat(newChat)
            pushToFirestore("chats", chatId, mapOf(
                "participantsCSV" to newChat.participantsCSV,
                "lastMessage" to newChat.lastMessage,
                "lastUpdated" to newChat.lastUpdated
            ))
            
            // Add initial welcome msg
            val welcomeMsg = ChatMessageEntity(
                id = UUID.randomUUID().toString(),
                chatId = chatId,
                senderId = providerId,
                senderName = providerName,
                text = "مرحباً بك! أنا مستعد لخدمتك ومناقشة تفاصيل العمل المطلوبة. يرجى كتابة تفاصيل طلبك.",
                timestamp = System.currentTimeMillis()
            )
            db.chatDao().insertMessage(welcomeMsg)
            pushToFirestore("chat_messages", welcomeMsg.id, mapOf(
                "chatId" to welcomeMsg.chatId,
                "senderId" to welcomeMsg.senderId,
                "senderName" to welcomeMsg.senderName,
                "text" to welcomeMsg.text,
                "timestamp" to welcomeMsg.timestamp
            ))
        }
        return chatId
    }

    suspend fun sendMessage(chatId: String, text: String, senderId: String, senderName: String) {
        val message = ChatMessageEntity(
            id = UUID.randomUUID().toString(),
            chatId = chatId,
            senderId = senderId,
            senderName = senderName,
            text = text,
            timestamp = System.currentTimeMillis()
        )
        db.chatDao().insertMessage(message)
        db.chatDao().updateLastMessage(chatId, text, System.currentTimeMillis())
        
        pushToFirestore("chat_messages", message.id, mapOf(
            "chatId" to message.chatId,
            "senderId" to message.senderId,
            "senderName" to message.senderName,
            "text" to message.text,
            "timestamp" to message.timestamp
        ))
        pushToFirestore("chats", chatId, mapOf(
            "participantsCSV" to "User, $senderName",
            "lastMessage" to text,
            "lastUpdated" to message.timestamp
        ))
    }

    // Call Gemini generative AI for chat/assistant integration
    suspend fun askGemini(prompt: String, apiKey: String): String = withContext(Dispatchers.IO) {
        if (apiKey.isEmpty()) {
            return@withContext "خطأ: لم يتم ضبط مفتاح واجهة برمجة تطبيقات Gemini في الإعدادات."
        }
        
        try {
            val urlString = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey"
            val url = URL(urlString)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true
            
            // JSON Payload
            val requestJson = JSONObject()
            val contentsArray = JSONArray()
            val contentObj = JSONObject()
            val partsArray = JSONArray()
            val partObj = JSONObject()
            partObj.put("text", "أنت مساعد ذكي مدمج في تطبيق دليل الخدمات اليمني WAM Services. أجب باللغة العربية باحترافية وبشكل مختصر عن هذا السؤال الخاص بالصيانة أو الخدمات المنزلية في اليمن: $prompt")
            partsArray.put(partObj)
            contentObj.put("parts", partsArray)
            contentsArray.put(contentObj)
            requestJson.put("contents", contentsArray)
            
            OutputStreamWriter(conn.outputStream).use { writer ->
                writer.write(requestJson.toString())
                writer.flush()
            }
            
            val responseCode = conn.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader(InputStreamReader(conn.inputStream)).use { reader ->
                    val response = StringBuilder()
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        response.append(line)
                    }
                    
                    val responseJson = JSONObject(response.toString())
                    val candidates = responseJson.getJSONArray("candidates")
                    if (candidates.length() > 0) {
                        val firstCandidate = candidates.getJSONObject(0)
                        val content = firstCandidate.getJSONObject("content")
                        val parts = content.getJSONArray("parts")
                        if (parts.length() > 0) {
                            return@withContext parts.getJSONObject(0).getString("text")
                        }
                    }
                }
            } else {
                Log.e("AppRepository", "Gemini HTTP error code: $responseCode")
                return@withContext "عذراً، حدث خطأ أثناء الاتصال بالذكاء الاصطناعي (كود الخطأ: $responseCode)."
            }
        } catch (e: Exception) {
            Log.e("AppRepository", "Error calling Gemini", e)
            return@withContext "حدث خطأ في الاتصال: ${e.localizedMessage ?: "خطأ غير معروف"}"
        }
        return@withContext "عذراً، لم أستطع توليد رد في الوقت الحالي."
    }

    // Initialize mock starter data if database is empty
    suspend fun populateStarterDataIfNeeded() {
        val categoriesCount = allCategories.first().size
        if (categoriesCount == 0) {
            val starterCategories = listOf(
                CategoryEntity("plumbing", "سباكة وصيانة الأنابيب", "Plumbing & Piping", "🚰", 10),
                CategoryEntity("electricity", "كهرباء وتمديدات طاقة شمسية", "Electric & Solar", "⚡", 9),
                CategoryEntity("mobile", "برمجة وصيانة الموبايل", "Mobile Fixing", "📱", 8),
                CategoryEntity("cleaning", "تنظيف منازل ومكاتب", "House Cleaning", "🧹", 7),
                CategoryEntity("mechanic", "ميكانيك وصيانة السيارات", "Car Mechanic", "🚗", 6),
                CategoryEntity("ac", "تكييف وتبريد جفان", "AC & Cooling", "❄️", 5)
            )
            
            for (cat in starterCategories) {
                db.categoryDao().insertCategory(cat)
            }
            
            val starterProviders = listOf(
                ProviderEntity(
                    id = "p1",
                    fullName = "م. عصام الوجيه",
                    providerType = "plumbing",
                    phone = "771234567",
                    status = "approved",
                    district = "صنعاء - حدة",
                    details = "متخصص في صيانة الأنابيب، شبكات الصرف الصحي، وتركيب السخانات الشمسية بخبرة تصل إلى 10 سنوات.",
                    rate = 4.8,
                    reviewsCount = 18,
                    isVerified = true
                ),
                ProviderEntity(
                    id = "p2",
                    fullName = "علي السخرية",
                    providerType = "electricity",
                    phone = "775566778",
                    status = "approved",
                    district = "صنعاء - الصافية",
                    details = "إصلاح تمديدات الكهرباء المنزلية وتركيب منظومات الطاقة الشمسية وصيانة البطاريات بكفاءة وأمان.",
                    rate = 4.9,
                    reviewsCount = 25,
                    isVerified = true
                ),
                ProviderEntity(
                    id = "p3",
                    fullName = "أصيل الحيمي",
                    providerType = "mobile",
                    phone = "733445566",
                    status = "approved",
                    district = "تعز - شارع جمال",
                    details = "صيانة وتغيير شاشات وبطاريات كافة هواتف آيفون وسامسونج، مع توفر قطع غيار أصلية بضمان حقيقي.",
                    rate = 4.6,
                    reviewsCount = 14,
                    isVerified = true
                ),
                ProviderEntity(
                    id = "p4",
                    fullName = "أم أمجد للخدمات",
                    providerType = "cleaning",
                    phone = "711223344",
                    status = "approved",
                    district = "عدن - كريتر",
                    details = "تنظيف منازل، شقق مفروشة ومكاتب بأسعار مناسبة وبأمانة تامة.",
                    rate = 4.7,
                    reviewsCount = 12,
                    isVerified = false
                ),
                ProviderEntity(
                    id = "p5",
                    fullName = "أبو فهد الحرازي",
                    providerType = "mechanic",
                    phone = "772233445",
                    status = "approved",
                    district = "صنعاء - الستين",
                    details = "فحص سيارات بالكمبيوتر، صيانة المحركات والجير والفرامل مع الكشف الشامل السريع.",
                    rate = 4.9,
                    reviewsCount = 32,
                    isVerified = true
                )
            )
            
            for (prov in starterProviders) {
                db.providerDao().insertProvider(prov)
            }
            
            // Add an initial notification
            val initialNotif = NotificationEntity(
                id = "notif_welcome",
                title = "مرحباً بكم في WAM Services 👋",
                body = "دليل الخدمات الأول في اليمن لربطكم بفنيين محترفين وموثوقين لحظياً بدون إنترنت بمزامنة حية فريدة.",
                type = "general",
                targetId = ""
            )
            db.notificationDao().insertNotification(initialNotif)
        }
    }
}
