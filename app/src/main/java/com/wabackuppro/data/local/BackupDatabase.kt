package com.wabackuppro.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.wabackuppro.domain.models.BackupRecord

/**
 * BackupDatabase serves as the main Room Database for local app data storage.
 */
@Database(entities = [BackupRecord::class], version = 1, exportSchema = false)
abstract class BackupDatabase : RoomDatabase() {

    abstract fun backupRecordDao(): BackupRecordDao

    companion object {
        @Volatile
        private var INSTANCE: BackupDatabase? = null

        fun getDatabase(context: Context): BackupDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BackupDatabase::class.java,
                    "wa_backup_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
