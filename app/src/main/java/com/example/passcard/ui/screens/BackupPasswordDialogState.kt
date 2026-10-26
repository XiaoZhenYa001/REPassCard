package com.example.passcard.ui.screens

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.passcard.crypto.BackupPasswordError
import com.example.passcard.crypto.BackupPasswordPolicy

/** Credentials live only in the current operation, never in saved navigation state. */
@Stable
internal class BackupPasswordDialogState {
    var visible by mutableStateOf(false)
    var password by mutableStateOf("")
    var confirmation by mutableStateOf("")
    var manualEntry by mutableStateOf(true)
    var isUpload by mutableStateOf(true)
    var saveForBiometric by mutableStateOf(true)
    var confirmEmptyOverwrite by mutableStateOf(false)
    var confirmRestore by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)

    fun open(upload: Boolean, hasSavedPassword: Boolean) {
        clearCredentials()
        isUpload = upload
        manualEntry = !hasSavedPassword
        saveForBiometric = true
        confirmEmptyOverwrite = false
        confirmRestore = false
        errorMessage = null
        visible = true
    }

    fun usePassword() {
        clearCredentials()
        manualEntry = true
        errorMessage = null
    }

    fun clearCredentials() {
        password = ""
        confirmation = ""
    }

    fun finish() {
        visible = false
        clearCredentials()
        confirmEmptyOverwrite = false
        confirmRestore = false
        errorMessage = null
    }

    fun passwordError(): BackupPasswordError? =
        if (manualEntry && isUpload) BackupPasswordPolicy.validationError(password, confirmation) else null
}

internal fun backupPasswordErrorMessage(error: BackupPasswordError, zh: Boolean): String = when (error) {
    BackupPasswordError.BLANK -> if (zh) "备份密码不能只包含空白字符。" else "The backup password cannot contain only whitespace."
    BackupPasswordError.TOO_SHORT -> if (zh) "备份密码至少需要 15 个字符。" else "Use at least 15 characters for the backup password."
    BackupPasswordError.TOO_LONG -> if (zh) "备份密码不能超过 1024 个字符。" else "The backup password cannot exceed 1024 characters."
    BackupPasswordError.MISMATCH -> if (zh) "两次输入的备份密码不一致。" else "The two backup passwords do not match."
}
