package com.example.passcard

import android.app.Application
import com.example.passcard.data.AppDatabase

class PassCardApp : Application() {
    
    companion object {
        @Volatile
        private var databaseInstance: AppDatabase? = null
        
        fun getDatabase(): AppDatabase? = databaseInstance
    }
    
    override fun onCreate() {
        super.onCreate()
        
        // 在后台线程预初始化数据库
        Thread {
            try {
                databaseInstance = AppDatabase.getInstance(this)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }
}
