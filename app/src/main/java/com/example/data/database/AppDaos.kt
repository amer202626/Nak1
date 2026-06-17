package com.example.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Delete
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    // --- Categories ---
    @Query("SELECT * FROM categories ORDER BY `order` ASC")
    fun getAllCategoriesFlow(): Flow<List<CategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteCategoryById(id: String)

    // --- Providers ---
    @Query("SELECT * FROM service_providers WHERE isBlocked = 0")
    fun getAllActiveProvidersFlow(): Flow<List<ProviderEntity>>

    @Query("SELECT * FROM service_providers")
    fun getAllProvidersFlow(): Flow<List<ProviderEntity>>

    @Query("SELECT * FROM service_providers WHERE isRecommended = 1 AND isBlocked = 0")
    fun getRecommendedProvidersFlow(): Flow<List<ProviderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProvider(provider: ProviderEntity)

    @Update
    suspend fun updateProvider(provider: ProviderEntity)

    @Query("UPDATE service_providers SET isVerified = :isVerified, isPinned = :isPinned, isRecommended = :isRecommended, isSubscribed = :isSubscribed, subscriptionExpiry = :expiry WHERE id = :id")
    suspend fun updateProviderFeaturesDirect(id: String, isVerified: Boolean, isPinned: Boolean, isRecommended: Boolean, isSubscribed: Boolean, expiry: Long)

    @Query("DELETE FROM service_providers WHERE id = :id")
    suspend fun deleteProviderById(id: String)

    // --- Pending Providers ---
    @Query("SELECT * FROM pending_providers ORDER BY createdAt DESC")
    fun getAllPendingProvidersFlow(): Flow<List<PendingProviderEntity>>

    @Query("SELECT COUNT(*) FROM pending_providers WHERE status = 'pending'")
    fun getPendingCountFlow(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPendingProvider(pending: PendingProviderEntity)

    @Query("DELETE FROM pending_providers WHERE id = :id")
    suspend fun deletePendingProviderById(id: String)

    // --- Dynamic Settings ---
    @Query("SELECT * FROM app_settings WHERE id = 'master' LIMIT 1")
    fun getSettingsFlow(): Flow<AppSettingEntity?>

    @Query("SELECT * FROM app_settings WHERE id = 'master' LIMIT 1")
    suspend fun getSettingsDirect(): AppSettingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettings(settings: AppSettingEntity)

    // --- Banners ---
    @Query("SELECT * FROM banners WHERE isActive = 1 ORDER BY createdAt DESC")
    fun getActiveBannersFlow(): Flow<List<BannerEntity>>

    @Query("SELECT * FROM banners ORDER BY createdAt DESC")
    fun getAllBannersFlow(): Flow<List<BannerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBanner(banner: BannerEntity)

    @Query("DELETE FROM banners WHERE id = :id")
    suspend fun deleteBannerById(id: String)

    // --- Reports ---
    @Query("SELECT * FROM reports ORDER BY createdAt DESC")
    fun getAllReportsFlow(): Flow<List<ReportEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(report: ReportEntity)

    @Query("DELETE FROM reports WHERE id = :id")
    suspend fun deleteReportById(id: String)

    // --- Reviews ---
    @Query("SELECT * FROM reviews WHERE providerId = :providerId ORDER BY createdAt DESC")
    fun getReviewsForProviderFlow(providerId: String): Flow<List<ReviewEntity>>

    @Query("SELECT * FROM reviews ORDER BY createdAt DESC")
    fun getAllReviewsFlow(): Flow<List<ReviewEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReview(review: ReviewEntity)

    @Query("DELETE FROM reviews WHERE id = :id")
    suspend fun deleteReviewById(id: String)

    // --- Loyalty Points ---
    @Query("SELECT * FROM loyalty_points WHERE userId = :userId LIMIT 1")
    suspend fun getLoyaltyPointsForUser(userId: String): LoyaltyPointsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLoyaltyPoints(points: LoyaltyPointsEntity)

    // --- Chats ---
    @Query("SELECT * FROM chats ORDER BY lastUpdated DESC")
    fun getAllChatsFlow(): Flow<List<ChatEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChat(chat: ChatEntity)

    @Query("DELETE FROM chats WHERE chatId = :chatId")
    suspend fun deleteChatById(chatId: String)

    @Query("DELETE FROM chats")
    suspend fun deleteAllChats()

    // --- Messages ---
    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY timestamp ASC")
    fun getMessagesForChatFlow(chatId: String): Flow<List<MessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Query("DELETE FROM messages")
    suspend fun deleteAllMessages()

    // --- Activity Logs ---
    @Query("SELECT * FROM activity_logs ORDER BY timestamp DESC")
    fun getAllActivityLogsFlow(): Flow<List<ActivityLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivityLog(log: ActivityLogEntity)

    // --- Registration Terms ---
    @Query("SELECT * FROM registration_terms WHERE isActive = 1 ORDER BY `order` ASC")
    fun getActiveRegistrationTermsFlow(): Flow<List<RegistrationTermEntity>>

    @Query("SELECT * FROM registration_terms ORDER BY `order` ASC")
    fun getAllRegistrationTermsFlow(): Flow<List<RegistrationTermEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRegistrationTerm(term: RegistrationTermEntity)

    @Query("DELETE FROM registration_terms WHERE id = :id")
    suspend fun deleteRegistrationTermById(id: String)

    // --- Bookings ---
    @Query("SELECT * FROM bookings ORDER BY createdAt DESC")
    fun getAllBookingsFlow(): Flow<List<BookingEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBooking(booking: BookingEntity)

    @Query("UPDATE bookings SET status = :status WHERE id = :id")
    suspend fun updateBookingStatus(id: String, status: String)

    @Query("DELETE FROM bookings WHERE id = :id")
    suspend fun deleteBookingById(id: String)

    // --- Notifications ---
    @Query("SELECT * FROM notifications ORDER BY createdAt DESC")
    fun getAllNotificationsFlow(): Flow<List<NotificationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationEntity)

    @Query("DELETE FROM notifications WHERE id = :id")
    suspend fun deleteNotificationById(id: String)

    @Query("DELETE FROM notifications")
    suspend fun deleteAllNotifications()
}
