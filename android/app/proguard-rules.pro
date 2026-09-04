# ProGuard rules for HabitMiner

# Keep Room entity and DAO classes
-keep class com.habitminer.data.** { *; }

# Keep WorkManager worker classes
-keep class com.habitminer.worker.** { *; }

# Keep Kotlin coroutines
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}
