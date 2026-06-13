package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String = "",
    val nameAr: String = "",
    val nameEn: String = "",
    val imageUrl: String = "",
    val parentId: String? = null,
    val order: Int = 0,
    val isActive: Boolean = true
)

@Entity(tableName = "service_providers")
data class ProviderEntity(
    @PrimaryKey val id: String = "",
    val fullName: String = "",
    val phone: String = "",
    val mainCategoryId: String = "",
    val subCategoryId: String = "",
    val address: String = "",
    val district: String = "",
    val locationLat: Double = 0.0,
    val locationLng: Double = 0.0,
    val profileImageUrl: String = "",
    val idCardImageUrl: String = "",
    val isPinned: Boolean = false,
    val isRecommended: Boolean = false,
    val isVerified: Boolean = false,
    val isBlocked: Boolean = false,
    val averageRating: Double = 5.0,
    val totalReviews: Int = 0,
    val isSubscribed: Boolean = false,
    val subscriptionExpiry: Long = 0L,
    val fcmToken: String = "",
    val createdAt: Long = 0L,
    val workImagesCSV: String = ""
)

@Entity(tableName = "pending_providers")
data class PendingProviderEntity(
    @PrimaryKey val id: String = "",
    val fullName: String = "",
    val phone: String = "",
    val mainCategoryId: String = "",
    val subCategoryId: String = "",
    val address: String = "",
    val district: String = "",
    val locationLat: Double = 0.0,
    val locationLng: Double = 0.0,
    val profileImageUrl: String = "",
    val idCardImageUrl: String = "",
    val status: String = "pending", // "pending", "rejected"
    val rejectReason: String = "",
    val createdAt: Long = 0L,
    val workImagesCSV: String = ""
)

@Entity(tableName = "app_settings")
data class AppSettingEntity(
    @PrimaryKey val id: String = "master",
    val appName: String = "",
    val welcomeMessage: String = "",
    val footerText: String = "",
    val supportPhone: String = "",
    val supportEmail: String = "",
    val supportWhatsApp: String = "",
    val primaryColor: String = "", // Hex code
    val secondaryColor: String = "", // Hex code
    val fontFamily: String = "Tajawal",
    val fontSize: Int = 14,
    val chatEnabled: Boolean = true,
    val assistantEnabled: Boolean = true,
    val radiusSearchLimit: Int = 50,
    val voiceSearchEnabled: Boolean = true,
    val maintenanceMode: Boolean = false,
    val maintenanceMessage: String = "",
    val ratingWeight: Double = 1.0,
    val maxWorkImages: Int = 3,
    val isMapsEnabled: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val aiIconSize: Int = 40,
    val aiIconX: Float = 0.85f,
    val aiIconY: Float = 0.85f,
    val footerSize: Int = 12,
    val isAiFloating: Boolean = true,
    val footerLogoUri: String = "",
    val customColorsCSV: String = "🌌 كوزميك سيلفر:#ECEFF1:#37474F,✨ الذهبي الفاخر:#FFFFD7:#8A7300,🟢 الزمردي الراقي:#2E7D32:#0D5215,⚫ الأسود الدخاني:#1A1A1A:#424242,🌸 الزهري الفاتح:#FFC0CB:#FF80AB,🟡 الأبيض الذهبي:#FFFDD0:#D4AF37"
)

@Entity(tableName = "banners")
data class BannerEntity(
    @PrimaryKey val id: String = "",
    val title: String = "",
    val type: String = "image", // "image", "video", "text"
    val mediaUrl: String = "",
    val redirectLink: String = "",
    val size: String = "M", // "S", "M", "L"
    val durationSeconds: Int = 5,
    val isActive: Boolean = true,
    val createdAt: Long = 0L
)

@Entity(tableName = "reports")
data class ReportEntity(
    @PrimaryKey val id: String = "",
    val providerId: String = "",
    val userId: String = "",
    val reason: String = "",
    val details: String = "",
    val status: String = "pending", // "pending", "resolved"
    val createdAt: Long = 0L
)

@Entity(tableName = "reviews")
data class ReviewEntity(
    @PrimaryKey val id: String = "",
    val providerId: String = "",
    val userId: String = "",
    val rating: Int = 5,
    val comment: String = "",
    val createdAt: Long = 0L
)

@Entity(tableName = "loyalty_points")
data class LoyaltyPointsEntity(
    @PrimaryKey val userId: String = "",
    val points: Int = 0
)

@Entity(tableName = "chats")
data class ChatEntity(
    @PrimaryKey val chatId: String = "",
    val participantsCSV: String = "",
    val lastMessage: String = "",
    val lastUpdated: Long = 0L
)

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val messageId: String = "",
    val chatId: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val message: String = "",
    val timestamp: Long = 0L,
    val isRead: Boolean = false
)

@Entity(tableName = "activity_logs")
data class ActivityLogEntity(
    @PrimaryKey val id: String = "",
    val adminId: String = "",
    val action: String = "",
    val details: String = "",
    val timestamp: Long = 0L
)

@Entity(tableName = "registration_terms")
data class RegistrationTermEntity(
    @PrimaryKey val id: String = "",
    val termText: String = "",
    val order: Int = 0,
    val isActive: Boolean = true
)
