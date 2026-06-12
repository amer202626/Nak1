package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val nameAr: String,
    val nameEn: String,
    val imageUrl: String,
    val parentId: String?,
    val order: Int,
    val isActive: Boolean
)

@Entity(tableName = "service_providers")
data class ProviderEntity(
    @PrimaryKey val id: String,
    val fullName: String,
    val phone: String,
    val mainCategoryId: String,
    val subCategoryId: String,
    val address: String,
    val district: String,
    val locationLat: Double,
    val locationLng: Double,
    val profileImageUrl: String,
    val idCardImageUrl: String,
    val isPinned: Boolean,
    val isRecommended: Boolean,
    val isVerified: Boolean,
    val isBlocked: Boolean,
    val averageRating: Double,
    val totalReviews: Int,
    val isSubscribed: Boolean,
    val subscriptionExpiry: Long,
    val fcmToken: String,
    val createdAt: Long
)

@Entity(tableName = "pending_providers")
data class PendingProviderEntity(
    @PrimaryKey val id: String,
    val fullName: String,
    val phone: String,
    val mainCategoryId: String,
    val subCategoryId: String,
    val address: String,
    val district: String,
    val locationLat: Double,
    val locationLng: Double,
    val profileImageUrl: String,
    val idCardImageUrl: String,
    val status: String, // "pending", "rejected"
    val rejectReason: String,
    val createdAt: Long
)

@Entity(tableName = "app_settings")
data class AppSettingEntity(
    @PrimaryKey val id: String = "master",
    val appName: String,
    val welcomeMessage: String,
    val footerText: String,
    val supportPhone: String,
    val supportEmail: String,
    val supportWhatsApp: String,
    val primaryColor: String, // Hex code like #ECEFF1 (Cosmic Silver default)
    val secondaryColor: String, // Hex code like #37474F
    val fontFamily: String,
    val fontSize: Int,
    val chatEnabled: Boolean,
    val assistantEnabled: Boolean,
    val radiusSearchLimit: Int,
    val voiceSearchEnabled: Boolean,
    val maintenanceMode: Boolean,
    val maintenanceMessage: String
)

@Entity(tableName = "banners")
data class BannerEntity(
    @PrimaryKey val id: String,
    val title: String,
    val type: String, // "image", "video", "text"
    val mediaUrl: String,
    val redirectLink: String,
    val size: String, // "S", "M", "L"
    val durationSeconds: Int,
    val isActive: Boolean,
    val createdAt: Long
)

@Entity(tableName = "reports")
data class ReportEntity(
    @PrimaryKey val id: String,
    val providerId: String,
    val userId: String,
    val reason: String,
    val details: String,
    val status: String, // "pending", "resolved"
    val createdAt: Long
)

@Entity(tableName = "reviews")
data class ReviewEntity(
    @PrimaryKey val id: String,
    val providerId: String,
    val userId: String,
    val rating: Int,
    val comment: String,
    val createdAt: Long
)

@Entity(tableName = "loyalty_points")
data class LoyaltyPointsEntity(
    @PrimaryKey val userId: String,
    val points: Int
)

@Entity(tableName = "chats")
data class ChatEntity(
    @PrimaryKey val chatId: String,
    val participantsCSV: String, // JSON or comma-separated user IDs
    val lastMessage: String,
    val lastUpdated: Long
)

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val messageId: String,
    val chatId: String,
    val senderId: String,
    val receiverId: String,
    val message: String,
    val timestamp: Long,
    val isRead: Boolean
)

@Entity(tableName = "activity_logs")
data class ActivityLogEntity(
    @PrimaryKey val id: String,
    val adminId: String,
    val action: String,
    val details: String,
    val timestamp: Long
)

@Entity(tableName = "registration_terms")
data class RegistrationTermEntity(
    @PrimaryKey val id: String,
    val termText: String,
    val order: Int,
    val isActive: Boolean
)
