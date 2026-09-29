package com.habitminer.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        AppUsageEntity::class,
        ContextSnapshotEntity::class,
        DiscoveredHabitEntity::class,
        BaselineEntity::class,
        DeviationEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appUsageDao(): AppUsageDao

    abstract fun contextDao(): ContextDao

    abstract fun habitDao(): HabitDao

    abstract fun baselineDao(): BaselineDao

    abstract fun deviationDao(): DeviationDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                val newInstance =
                    Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        "habitminer_database",
                    )
                        .fallbackToDestructiveMigration()
                        .build()
                instance = newInstance
                newInstance
            }
        }
    }
}
