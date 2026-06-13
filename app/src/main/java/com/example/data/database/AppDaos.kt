package com.example.data.database

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
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
}
