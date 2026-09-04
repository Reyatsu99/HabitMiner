package com.habitminer.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [RawGpsEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun locationDao(): LocationDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "habitminer_db"
                )
                // Note: SQLCipher integration for encryption can be added here in the future
                // .openHelperFactory(SupportFactory(passphrase))
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
