package com.chatbotai.app.model

// role: "user" atau "model" (ai)
data class Message(
    val id: Long = 0,
    val sessionId: Long,
    val role: String,
    var content: String,
    val imagePath: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

// Dipakai adapter buat nampilin bubble "AI lagi ngetik..."
const val ROLE_USER = "user"
const val ROLE_MODEL = "model"
const val ROLE_TYPING = "typing"
