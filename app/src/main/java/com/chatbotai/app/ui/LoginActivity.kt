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

class LoginActivity : AppCompatActivity() {

    private lateinit var repository: ChatRepository
    private lateinit var prefs: Prefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        repository = ChatRepository(this)
        prefs = Prefs(this)

        // Kalau udah login sebelumnya, langsung lempar ke ChatActivity
        if (prefs.loggedInUserId != -1L) {
            startActivity(Intent(this, ChatActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_login)

        val etUsername = findViewById<EditText>(R.id.etUsername)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val tvError = findViewById<TextView>(R.id.tvError)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val tvGoRegister = findViewById<TextView>(R.id.tvGoRegister)

        btnLogin.setOnClickListener {
            val username = etUsername.text.toString().trim()
            val password = etPassword.text.toString()

            if (username.isEmpty() || password.isEmpty()) {
                tvError.text = "Username & password wajib diisi"
                tvError.visibility = TextView.VISIBLE
                return@setOnClickListener
            }

            val userId = repository.loginUser(username, password)
            if (userId == null) {
                tvError.text = "Username atau password salah"
                tvError.visibility = TextView.VISIBLE
            } else {
                prefs.loggedInUserId = userId
                prefs.loggedInUsername = username
                startActivity(Intent(this, ChatActivity::class.java))
                finish()
            }
        }

        tvGoRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }
}
