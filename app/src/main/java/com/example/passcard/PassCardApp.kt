package com.example.passcard

import android.app.Application
import com.example.passcard.data.AutomaticBackupKeyStore
import com.example.passcard.sync.AutomaticBackupScheduler
import com.example.passcard.util.AutoUploadFrequency
import com.example.passcard.util.PreferencesManager
import com.example.passcard.util.SyncSecurityMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class PassCardApp : Application() {
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            val preferences = PreferencesManager(this@PassCardApp)
            val frequency = preferences.autoUploadFrequency
            val eligible = frequency != AutoUploadFrequency.OFF &&
                preferences.syncSecurityMode == SyncSecurityMode.CONVENIENCE &&
                AutomaticBackupKeyStore.hasKey(this@PassCardApp)
            AutomaticBackupScheduler.reconcile(
                context = this@PassCardApp,
                frequency = if (eligible) frequency else AutoUploadFrequency.OFF
            )
        }
    }
}
