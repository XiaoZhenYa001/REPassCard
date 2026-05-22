package com.example.passcard.util

import android.content.Context
import com.example.passcard.data.AppDatabase
import java.io.File
import java.security.KeyStore

object AppResetHelper {
    fun resetAllData(context: Context) {
        val appContext = context.applicationContext
        
        // 1. Wipe SharedPreferences
        appContext.getSharedPreferences("repasscard_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        appContext.getSharedPreferences("repasscard_database_crypto", Context.MODE_PRIVATE).edit().clear().commit()
        appContext.getSharedPreferences("repasscard_vault_crypto", Context.MODE_PRIVATE).edit().clear().commit()
        
        // 2. Wipe Databases
        val dbFile = appContext.getDatabasePath("passcard_database")
        if (dbFile.exists()) {
            dbFile.delete()
            File("${dbFile.absolutePath}-journal").delete()
            File("${dbFile.absolutePath}-shm").delete()
            File("${dbFile.absolutePath}-wal").delete()
        }
        
        // 3. Clear KeyStore aliases (both DB passphrase and Cloud Sync Biometric keys)
        try {
            val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            if (keyStore.containsAlias("repasscard_database_passphrase_key")) {
                keyStore.deleteEntry("repasscard_database_passphrase_key")
            }
            if (keyStore.containsAlias("repasscard_vault_key")) {
                keyStore.deleteEntry("repasscard_vault_key")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
