package com.example.passcard.sync

import android.content.Context
import android.os.Build
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.passcard.data.AppDatabase
import com.example.passcard.data.AutomaticBackupKeyStore
import com.example.passcard.util.AutoUploadFrequency
import com.example.passcard.util.AutoUploadStatus
import com.example.passcard.util.PreferencesManager
import com.example.passcard.util.SyncSecurityMode

class AutomaticBackupWorker(
    appContext: Context,
    workerParameters: WorkerParameters
) : CoroutineWorker(appContext, workerParameters) {

    override suspend fun doWork(): Result {
        val preferences = PreferencesManager(applicationContext)
        if (preferences.autoUploadFrequency == AutoUploadFrequency.OFF) return Result.success()
        if (preferences.syncSecurityMode != SyncSecurityMode.CONVENIENCE) {
            return stopForUser(preferences, "安全模式已变化，请重新授权自动上传")
        }
        if (!hasCloudConfiguration(preferences)) {
            return stopForUser(preferences, "云端连接配置不完整")
        }
        val backupPassword = AutomaticBackupKeyStore.read(applicationContext)
            ?: return stopForUser(preferences, "自动上传密钥不可用，请重新授权")

        return try {
            val passwords = AppDatabase.getInstance(applicationContext)
                .passwordDao()
                .getAllPasswordsSnapshot()
            val repository = CloudSyncRepository(
                applicationContext,
                preferences,
                S3CloudStorage(
                    endpoint = preferences.s3Endpoint,
                    region = preferences.s3Region,
                    bucketName = preferences.s3Bucket,
                    accessKey = preferences.s3AccessKey,
                    secretKey = preferences.s3SecretKey,
                    sessionToken = preferences.s3SessionToken
                )
            )
            val snapshot = repository.fetchCloudSnapshot(passwords.size)
            when (AutomaticBackupPolicy.decide(snapshot, preferences.lastSyncedCloudVaultRevision)) {
                AutomaticBackupDecision.SKIP_EMPTY_LOCAL -> {
                    preferences.recordAutoUpload(AutoUploadStatus.SKIPPED_EMPTY, "本地保险库为空，已跳过上传")
                    Result.success()
                }
                AutomaticBackupDecision.STOP_FOR_CLOUD_CONFLICT -> {
                    preferences.recordAutoUpload(AutoUploadStatus.CONFLICT, "云端包含较新修订，需要手动处理")
                    Result.success()
                }
                AutomaticBackupDecision.UPLOAD -> {
                    repository.uploadEncryptedVault(
                        backupPassword = backupPassword,
                        passwords = passwords,
                        deviceId = preferences.getOrCreateInstallationId(),
                        deviceName = "${Build.MANUFACTURER} ${Build.MODEL}".trim(),
                        userConfirmedEmptyOverwrite = false,
                        loadPasswords = {
                            AppDatabase.getInstance(applicationContext).passwordDao().getAllPasswordsSnapshot()
                                .also { if (it.isEmpty()) throw EmptyAutomaticVaultException() }
                        }
                    ).getOrThrow()
                    preferences.recordAutoUpload(AutoUploadStatus.SUCCESS, "加密备份已自动上传")
                    Result.success()
                }
            }
        } catch (error: EmptyAutomaticVaultException) {
            preferences.recordAutoUpload(AutoUploadStatus.SKIPPED_EMPTY, "本地保险库为空，已跳过上传")
            Result.success()
        } catch (error: CloudWriteConflictException) {
            preferences.recordAutoUpload(AutoUploadStatus.CONFLICT, "云端备份已更新，请先保留本地记录并手动处理")
            Result.success()
        } catch (error: javax.crypto.BadPaddingException) {
            preferences.recordAutoUpload(AutoUploadStatus.ERROR, "云端备份密码不匹配，请重新授权自动上传")
            Result.failure()
        } catch (error: CloudConditionalWriteCheckException) {
            preferences.recordAutoUpload(AutoUploadStatus.ERROR, error.detail)
            val transient = error.reason == CloudConditionalWriteFailure.REQUEST_FAILED ||
                (error.reason == CloudConditionalWriteFailure.HTTP_ERROR &&
                    error.httpStatus?.let { it in 500..599 && it != 501 } == true)
            if (transient && runAttemptCount < MAX_RETRIES) Result.retry() else Result.failure()
        } catch (error: Throwable) {
            preferences.recordAutoUpload(
                AutoUploadStatus.ERROR,
                error.message?.takeIf { it.isNotBlank() } ?: "自动上传失败"
            )
            if (runAttemptCount < MAX_RETRIES) Result.retry() else Result.failure()
        }
    }

    private fun stopForUser(preferences: PreferencesManager, message: String): Result {
        preferences.recordAutoUpload(AutoUploadStatus.ERROR, message)
        return Result.failure()
    }

    private fun hasCloudConfiguration(preferences: PreferencesManager): Boolean =
        preferences.s3Endpoint.isNotBlank() && preferences.s3Bucket.isNotBlank() &&
            preferences.s3AccessKey.isNotBlank() && preferences.s3SecretKey.isNotBlank()

    private class EmptyAutomaticVaultException : IllegalStateException()

    companion object {
        private const val MAX_RETRIES = 3
    }
}
