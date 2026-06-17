package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        CategoryEntity::class,
        ProviderEntity::class,
        PendingProviderEntity::class,
        AppSettingEntity::class,
        BannerEntity::class,
        ReportEntity::class,
        ReviewEntity::class,
        LoyaltyPointsEntity::class,
        ChatEntity::class,
        MessageEntity::class,
        ActivityLogEntity::class,
        RegistrationTermEntity::class,
        BookingEntity::class,
        NotificationEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "wam_services_db"
                )
                .fallbackToDestructiveMigration()
                .addCallback(DatabaseCallback(context))
                .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(private val context: Context) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                // Pre-populate database with default configurations, categories, and terms
                CoroutineScope(Dispatchers.IO).launch {
                    val dao = getDatabase(context).appDao()
                    
                    // App Settings default configuration (Cosmic Silver setup)
                    dao.insertSettings(
                        AppSettingEntity(
                            id = "master",
                            appName = "دليل خدمات اليمن",
                            welcomeMessage = "أهلاً ومرحباً بكم مع تطبيق كل خدمات اليمن - دليل أصحاب المهن الفاخر",
                            footerText = "MAW 777644670",
                            supportPhone = "777644670",
                            supportEmail = "support@wam2026.com",
                            supportWhatsApp = "+967777644670",
                            primaryColor = "#ECEFF1", // Cosmic Silver default
                            secondaryColor = "#37474F",
                            fontFamily = "Cairo",
                            fontSize = 14,
                            chatEnabled = true,
                            assistantEnabled = true,
                            radiusSearchLimit = 10,
                            voiceSearchEnabled = true,
                            maintenanceMode = false,
                            maintenanceMessage = "التطبيق قيد الصيانة الدورية حالياً. نعتذر عن الإزعاج."
                        )
                    )

                    // Default main categories for Yemen Service Direct
                    val sampleCategories = listOf(
                        CategoryEntity("c1", "سباكة", "Plumbing", "🔧", null, 1, true),
                        CategoryEntity("c2", "كهرباء", "Electrical", "⚡", null, 2, true),
                        CategoryEntity("c3", "دهان", "Painting", "🎨", null, 3, true),
                        CategoryEntity("c4", "نجارة", "Carpentry", "🔨", null, 4, true),
                        CategoryEntity("c5", "حدادة", "Smithing", "⚙️", null, 5, true),
                        
                        CategoryEntity("sub1", "تصليح مواسير", "Pipe Repairs", "", "c1", 1, true),
                        CategoryEntity("sub2", "تركيب مغاسل", "Sink Installation", "", "c1", 2, true),
                        CategoryEntity("sub3", "تمديد كابلات", "Cable Extension", "", "c2", 1, true),
                        CategoryEntity("sub4", "دهان جدران", "Wall Painting", "", "c3", 1, true)
                    )
                    sampleCategories.forEach { dao.insertCategory(it) }

                    // Default providers for demo
                    val sampleProviders = listOf(
                        ProviderEntity(
                            id = "p1",
                            fullName = "ماهر محمد طاهر",
                            phone = "777644670",
                            mainCategoryId = "c1",
                            subCategoryId = "sub1",
                            address = "صنعاء - شارع حدة",
                            district = "منطقة السبعين",
                            locationLat = 15.3186,
                            locationLng = 44.2045,
                            profileImageUrl = "",
                            idCardImageUrl = "",
                            isPinned = true,
                            isRecommended = true,
                            isVerified = true,
                            isBlocked = false,
                            averageRating = 5.0,
                            totalReviews = 1,
                            isSubscribed = true,
                            subscriptionExpiry = System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000),
                            fcmToken = "",
                            createdAt = System.currentTimeMillis()
                        ),
                        ProviderEntity(
                            id = "p2",
                            fullName = "علي أحمد الكبسي",
                            phone = "733829103",
                            mainCategoryId = "c2",
                            subCategoryId = "sub3",
                            address = "صنعاء - الدائري الغربي",
                            district = "معين",
                            locationLat = 15.3456,
                            locationLng = 44.1812,
                            profileImageUrl = "",
                            idCardImageUrl = "",
                            isPinned = false,
                            isRecommended = false,
                            isVerified = true,
                            isBlocked = false,
                            averageRating = 4.5,
                            totalReviews = 12,
                            isSubscribed = false,
                            subscriptionExpiry = 0L,
                            fcmToken = "",
                            createdAt = System.currentTimeMillis()
                        )
                    )
                    sampleProviders.forEach { dao.insertProvider(it) }

                    // Initial standard terms of sign up (Registration Terms)
                    val sampleTerms = listOf(
                        RegistrationTermEntity("term1", "الالتزام بالصدق والأمانة في التعامل وتحديد الأسعار المناسبة.", 1, true),
                        RegistrationTermEntity("term2", "أن يكون المهني حاصلاً على الخبرة الميدانية المؤكدة في تخصص خدمته.", 2, true),
                        RegistrationTermEntity("term3", "صورة بطاقة الهوية إجبارية لأجل توثيق الشارة الزرقاء وتفعيل الضمان للمستخدم.", 3, true)
                    )
                    sampleTerms.forEach { dao.insertRegistrationTerm(it) }

                    // Demo banners
                    dao.insertBanner(
                        BannerEntity(
                            id = "banner1",
                            title = "عرض الافتتاح الكبير لدليل خدمات اليمن",
                            type = "text",
                            mediaUrl = "",
                            redirectLink = "",
                            size = "M",
                            durationSeconds = 5,
                            isActive = true,
                            createdAt = System.currentTimeMillis()
                        )
                    )
                }
            }
        }
    }
}
