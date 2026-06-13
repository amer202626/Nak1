package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.*

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
        RegistrationTermEntity::class
    ],
    version = 1,
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
                    "wam_services_local_db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
