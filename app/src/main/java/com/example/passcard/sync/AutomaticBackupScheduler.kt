package com.example.passcard.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.passcard.util.AutoUploadFrequency
import java.util.concurrent.TimeUnit

object AutomaticBackupScheduler {
    private const val UNIQUE_WORK_NAME = "passcard_automatic_encrypted_backup"

    fun reconcile(context: Context, frequency: AutoUploadFrequency, replace: Boolean = false) {
        val workManager = WorkManager.getInstance(context.applicationContext)
        if (frequency == AutoUploadFrequency.OFF) {
            workManager.cancelUniqueWork(UNIQUE_WORK_NAME)
            return
        }
        val request = PeriodicWorkRequestBuilder<AutomaticBackupWorker>(
            frequency.intervalDays,
            TimeUnit.DAYS
        )
            .setInitialDelay(frequency.intervalDays, TimeUnit.DAYS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .setRequiresBatteryNotLow(true)
                    .setRequiresStorageNotLow(true)
                    .build()
            )
            .build()
        workManager.enqueueUniquePeriodicWork(
            UNIQUE_WORK_NAME,
            if (replace) ExistingPeriodicWorkPolicy.UPDATE else ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}
