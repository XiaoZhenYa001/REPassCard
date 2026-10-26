package com.example.passcard.util

import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricManager
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import java.util.concurrent.Executor
import javax.crypto.Cipher

object AuthHelper {
    fun authenticateWithCipher(
        activity: FragmentActivity,
        cipher: Cipher,
        title: String,
        subtitle: String,
        onSuccess: (Cipher) -> Unit,
        onError: (String) -> Unit,
        negativeButtonText: String = "Cancel"
    ): BiometricPrompt {
        val executor: Executor = ContextCompat.getMainExecutor(activity)
        val biometricPrompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    onError(errString.toString())
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    val outCipher = result.cryptoObject?.cipher
                    if (outCipher != null) {
                        onSuccess(outCipher)
                    } else {
                        onError("CryptoObject is null")
                    }
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    // The system prompt shows retry feedback. Only terminal errors close this attempt.
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .setNegativeButtonText(negativeButtonText)
            .build()

        biometricPrompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(cipher))
        return biometricPrompt
    }
}