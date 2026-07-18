package com.chatbotai.app.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.chatbotai.app.R
import com.chatbotai.app.db.ChatRepository
import com.chatbotai.app.util.Prefs

class RegisterActivity : AppCompatActivity() {

    private lateinit var repository: ChatRepository
    private lateinit var prefs: Prefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        repository = ChatRepository(this)
        prefs = Prefs(this)

        val etUsername = findViewById<EditText>(R.id.etUsername)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val etConfirmPassword = findViewById<EditText>(R.id.etConfirmPassword)
        val tvError = findViewById<TextView>(R.id.tvError)
        val btnRegister = findViewById<Button>(R.id.btnRegister)
        val tvGoLogin = findViewById<TextView>(R.id.tvGoLogin)

        btnRegister.setOnClickListener {
            val username = etUsername.text.toString().trim()
            val password = etPassword.text.toString()
            val confirmPassword = etConfirmPassword.text.toString()

            when {
                username.length < 3 -> {
                    showError(tvError, "Username minimal 3 karakter")
                }
                password.length < 4 -> {
                    showError(tvError, "Password minimal 4 karakter")
                }
                password != confirmPassword -> {
                    showError(tvError, "Konfirmasi password tidak cocok")
                }
                else -> {
                    val userId = repository.registerUser(username, password)
                    if (userId == null) {
                        showError(tvError, "Username sudah dipakai, coba yang lain")
                    } else {
                        prefs.loggedInUserId = userId
                        prefs.loggedInUsername = username
                        startActivity(Intent(this, ChatActivity::class.java))
                        finish()
                    }
                }
            }
        }

        tvGoLogin.setOnClickListener {
            finish()
        }
    }

    private fun showError(tv: TextView, msg: String) {
        tv.text = msg
        tv.visibility = TextView.VISIBLE
    }
}
