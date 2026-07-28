package com.wabackuppro.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.wabackuppro.data.local.daos.BackupFileEntryDao
import com.wabackuppro.data.local.daos.BackupFileResultDao
import com.wabackuppro.data.local.daos.BackupRecordDao
import com.wabackuppro.data.local.entities.BackupFileEntry
import com.wabackuppro.data.local.entities.BackupFileResult
import com.wabackuppro.data.local.entities.BackupRecord

/**
 * AppDatabase provides the main entry point to the Room database.
 * Updated to version 3 to support drill-down per-file execution results.
 */
@Database(
    entities = [BackupRecord::class, BackupFileEntry::class, BackupFileResult::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun backupRecordDao(): BackupRecordDao
    abstract fun backupFileEntryDao(): BackupFileEntryDao
    abstract fun backupFileResultDao(): BackupFileResultDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "wabackuppro_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
