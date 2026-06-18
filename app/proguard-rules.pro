# Add project specific ProGuard rules here.
# By default, the ProGuard rules in this file are appended automatically by Android Gradle plugin.
# Keep Room database classes
-keep class * extends androidx.room.RoomDatabase
-keep class * extends androidx.room.RoomDatabase$Callback
