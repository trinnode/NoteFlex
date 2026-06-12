package com.noteflex.overlay

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.core.content.ContextCompat
import java.security.MessageDigest

class AuthActivity : AppCompatActivity() {

    private lateinit var tabId: String
    private var tabTitle: String = ""
    private var passwordHash: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(android.R.style.Theme_Translucent_NoTitleBar)

        tabId = intent?.getStringExtra("tabId") ?: run { finish(); return }
        tabTitle = intent?.getStringExtra("tabTitle") ?: "Note"
        passwordHash = intent?.getStringExtra("passwordHash")?.takeIf { it.isNotEmpty() }

        showBiometricPrompt()
    }

    private fun showBiometricPrompt() {
        val biometricManager = BiometricManager.from(this)
        val canAuth = biometricManager.canAuthenticate(
            BIOMETRIC_STRONG or DEVICE_CREDENTIAL
        )

        if (canAuth == BiometricManager.BIOMETRIC_SUCCESS ||
            canAuth == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED
        ) {
            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle("Unlock $tabTitle")
                .setSubtitle("Use your fingerprint or device password")
                .setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
                .build()

            val prompt = BiometricPrompt(
                this,
                ContextCompat.getMainExecutor(this),
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        AuthState.unlock(tabId)
                        setResult(RESULT_OK)
                        finish()
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        if (errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                            errorCode == BiometricPrompt.ERROR_USER_CANCELED
                        ) {
                            showPasswordFallback()
                        } else {
                            Toast.makeText(
                                this@AuthActivity,
                                "Authentication error: $errString",
                                Toast.LENGTH_SHORT
                            ).show()
                            finish()
                        }
                    }

                    override fun onAuthenticationFailed() {
                        Toast.makeText(
                            this@AuthActivity,
                            "Fingerprint not recognized",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            )
            prompt.authenticate(promptInfo)
        } else {
            showPasswordFallback()
        }
    }

    private fun showPasswordFallback() {
        if (passwordHash == null) {
            Toast.makeText(this, "No password set for this note", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val input = EditText(this).apply {
            hint = "Enter password"
            setSingleLine(true)
            inputType = android.text.InputType.TYPE_CLASS_TEXT or
                    android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }

        android.app.AlertDialog.Builder(this)
            .setTitle("Unlock $tabTitle")
            .setView(input)
            .setPositiveButton("Unlock") { _, _ ->
                val entered = input.text?.toString() ?: ""
                val enteredHash = sha256(entered)
                if (enteredHash == passwordHash) {
                    AuthState.unlock(tabId)
                    setResult(RESULT_OK)
                    finish()
                } else {
                    Toast.makeText(this, "Wrong password", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .setNegativeButton("Cancel") { _, _ -> finish() }
            .setOnCancelListener { finish() }
            .show()
    }

    companion object {
        fun sha256(input: String): String {
            val digest = MessageDigest.getInstance("SHA-256")
            val bytes = digest.digest(input.toByteArray())
            return bytes.joinToString("") { "%02x".format(it) }
        }

        fun hashPassword(input: String): String = sha256(input)
    }
}
