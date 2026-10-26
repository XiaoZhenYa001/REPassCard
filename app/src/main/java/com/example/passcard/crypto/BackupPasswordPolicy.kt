package com.example.passcard.crypto

enum class BackupPasswordError { BLANK, TOO_SHORT, TOO_LONG, MISMATCH }

/** Password text is always used exactly as entered: no trimming, case folding, or normalization. */
object BackupPasswordPolicy {
    const val MIN_CODE_POINTS = 15
    const val MAX_CODE_POINTS = 1024

    fun validationError(password: String, confirmation: String): BackupPasswordError? {
        val length = password.codePointCount(0, password.length)
        return when {
            password.isBlank() -> BackupPasswordError.BLANK
            length < MIN_CODE_POINTS -> BackupPasswordError.TOO_SHORT
            length > MAX_CODE_POINTS -> BackupPasswordError.TOO_LONG
            password != confirmation -> BackupPasswordError.MISMATCH
            else -> null
        }
    }

    /** Existing backups are decrypted verbatim, without applying the policy for newly chosen passwords. */
    fun requireUsablePassword(password: String) {
        require(password.isNotEmpty()) { "备份密码不能为空。" }
        require(password.codePointCount(0, password.length) <= MAX_CODE_POINTS) { "备份密码过长。" }
    }
}
