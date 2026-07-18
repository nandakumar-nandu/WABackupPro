package com.wabackuppro.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.wabackuppro.data.local.daos.BackupRecordDao
import com.wabackuppro.data.local.entities.BackupRecord

/**
 * AppDatabase provides the main entry point to the Room database.
 */
@Database(entities = [BackupRecord::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun backupRecordDao(): BackupRecordDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "wabackuppro_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
