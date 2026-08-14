package com.wabackuppro.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.wabackuppro.domain.models.BackupRecord

/**
 * Legacy Room Database declaration (Version 1, "wa_backup_database").
 *
 * @deprecated Legacy database schema from initial skeleton (v0.1.0). The active production database
 * is [com.wabackuppro.data.local.AppDatabase] (Version 3, "wabackuppro_database"). Retained for
 * potential historical upgrade references.
 */
@Deprecated(
    message = "Legacy v1 database. Active database is AppDatabase (Version 3, wabackuppro_database)."
)
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
