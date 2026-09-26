# Keep Compose / ViewModels
-keep class com.mute.shutter.** { *; }
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}

# WorkManager
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.CoroutineWorker

# AdMob (free flavor)
-keep class com.google.android.gms.ads.** { *; }
-keep class com.google.ads.** { *; }
-dontwarn com.google.android.gms.ads.**
