package com.chatbotai.app.util

import java.security.MessageDigest

/**
 * Hashing password pake SHA-256 + salt statis.
 * Catatan: ini cukup buat app lokal/personal. Kalau nanti app-nya
 * dipublish beneran & dipake banyak orang, ganti ke bcrypt/argon2
 * dan idealnya pindah login ke backend server, bukan cuma di device.
 */
object PasswordUtil {
    private const val SALT = "geminichat_local_salt_v1"

    fun hash(password: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest((SALT + password).toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
