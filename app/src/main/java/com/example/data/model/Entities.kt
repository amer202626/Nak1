package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val nameAr: String,
    val nameEn: String,
    val emoji: String,
    val priority: Int = 0
)

@Serializable
@Entity(tableName = "providers")
data class ProviderEntity(
    @PrimaryKey val id: String,
    val fullName: String,
    val providerType: String, // Category ID
    val phone: String,
    val status: String = "approved", // "pending" / "approved"
    val district: String,
    val details: String,
    val rate: Double = 4.5,
    val reviewsCount: Int = 10,
    val imageBase64: String = "",
    val isVerified: Boolean = false,
    val dateAdded: Long = System.currentTimeMillis()
)

@Serializable
@Entity(tableName = "bookings")
data class BookingEntity(
    @PrimaryKey val id: String,
    val providerId: String,
    val providerName: String,
    val fullName: String, // Beneficiary Full Name
    val phone: String, // WhatsApp & Calling number
    val district: String, // City & region
    val status: String = "pending", // "pending", "confirmed", "completed", "cancelled"
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey val id: String,
    val title: String,
    val body: String,
    val type: String = "general", // "general", "targeted_user"
    val targetId: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
@Entity(tableName = "chats")
data class ChatEntity(
    @PrimaryKey val chatId: String,
    val participantsCSV: String,
    val lastMessage: String,
    val lastUpdated: Long = System.currentTimeMillis()
)

@Serializable
@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey val id: String,
    val chatId: String,
    val senderId: String, // "user", "admin", "provider"
    val senderName: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)
