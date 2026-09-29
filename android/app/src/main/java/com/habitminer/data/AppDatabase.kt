package com.habitminer.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        AppUsageEntity::class,
        ContextSnapshotEntity::class,
        DiscoveredHabitEntity::class,
        BaselineEntity::class,
        DeviationEntity::class,
        DeviceEventEntity::class,
    ],
    version = 3,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appUsageDao(): AppUsageDao

    abstract fun contextDao(): ContextDao

    abstract fun habitDao(): HabitDao

    abstract fun baselineDao(): BaselineDao

    abstract fun deviationDao(): DeviationDao

    abstract fun deviceEventDao(): DeviceEventDao

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
                        .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                        .fallbackToDestructiveMigration()
                        .build()
                instance = newInstance
                newInstance
            }
        }

        private val MIGRATION_1_2 =
            object : Migration(1, 2) {
                override fun migrate(database: SupportSQLiteDatabase) {
                    database.execSQL(
                        "CREATE TABLE IF NOT EXISTS device_events (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "eventType TEXT NOT NULL, " +
                            "packageName TEXT, " +
                            "timestamp INTEGER NOT NULL)",
                    )
                    database.execSQL(
                        "CREATE INDEX IF NOT EXISTS index_device_events_timestamp ON device_events(timestamp)",
                    )
                }
            }

        private val MIGRATION_2_3 =
            object : Migration(2, 3) {
                override fun migrate(database: SupportSQLiteDatabase) {
                    // Drop the old single-column index on device_events and add the composite one
                    database.execSQL("DROP INDEX IF EXISTS index_device_events_timestamp")
                    database.execSQL(
                        "CREATE INDEX IF NOT EXISTS index_device_events_eventType_timestamp ON device_events(eventType, timestamp)",
                    )

                    // Add indices for app_usage
                    database.execSQL("CREATE INDEX IF NOT EXISTS index_app_usage_startTime_endTime ON app_usage(startTime, endTime)")
                    database.execSQL("CREATE INDEX IF NOT EXISTS index_app_usage_dayType_timeSlot ON app_usage(dayType, timeSlot)")
                    database.execSQL("CREATE INDEX IF NOT EXISTS index_app_usage_packageName ON app_usage(packageName)")

                    // Add index for context_snapshots
                    database.execSQL("CREATE INDEX IF NOT EXISTS index_context_snapshots_timestamp ON context_snapshots(timestamp)")

                    // Add index for deviations
                    database.execSQL("CREATE INDEX IF NOT EXISTS index_deviations_timestamp ON deviations(timestamp)")
                }
            }
    }
}
