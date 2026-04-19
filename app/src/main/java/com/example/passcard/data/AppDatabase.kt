package com.example.passcard.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [PasswordEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun passwordDao(): PasswordDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                // 使用内部存储（更快、更稳定）
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "passcard_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
        
        /**
         * 获取应用外部存储根目录（用于用户导出文件）
         * 路径: /storage/emulated/0/Android/data/com.example.passcard/files/
         */
        fun getAppDataDir(context: Context): java.io.File? {
            return context.getExternalFilesDir(null)
        }
    }
}
