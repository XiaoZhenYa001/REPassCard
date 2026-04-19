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
                // 使用应用外部存储目录，与其他应用保持一致
                val appDir = context.getExternalFilesDir(null)
                val dbFile = if (appDir != null) {
                    java.io.File(appDir, "databases/passcard_database")
                } else {
                    context.getDatabasePath("passcard_database")
                }
                
                // 确保数据库目录存在
                dbFile?.parentFile?.mkdirs()
                
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    dbFile?.absolutePath ?: "passcard_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
        
        /**
         * 获取应用外部存储根目录
         * 路径: /storage/emulated/0/Android/data/com.example.passcard/files/
         */
        fun getAppDataDir(context: Context): java.io.File? {
            return context.getExternalFilesDir(null)
        }
    }
}
