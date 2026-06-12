package com.noteflex.overlay

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.security.MessageDigest

class AuthActivity : AppCompatActivity() {

    private lateinit var tabId: String
    private var tabTitle: String = ""
    private var passwordHash: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        tabId = intent?.getStringExtra("tabId") ?: run { finish(); return }
        tabTitle = intent?.getStringExtra("tabTitle") ?: "Note"
        passwordHash = intent?.getStringExtra("passwordHash")?.takeIf { it.isNotEmpty() }

        showPasswordDialog()
    }

    private fun showPasswordDialog() {
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

        AlertDialog.Builder(this)
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