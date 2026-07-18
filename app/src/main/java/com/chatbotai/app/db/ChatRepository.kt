package com.chatbotai.app.db

import android.content.Context
import com.chatbotai.app.model.ChatSession
import com.chatbotai.app.model.Message
import com.chatbotai.app.util.PasswordUtil

class ChatRepository(context: Context) {

    private val helper = DbHelper(context.applicationContext)

    // ---------- USER ----------

    /** Return null kalau username udah dipake, atau userId baru kalau sukses */
    fun registerUser(username: String, password: String): Long? {
        val db = helper.writableDatabase
        val cursor = db.query(
            DbHelper.TABLE_USERS, arrayOf("id"), "username=?",
            arrayOf(username), null, null, null
        )
        val exists = cursor.count > 0
        cursor.close()
        if (exists) return null

        val values = cv(
            "username" to username,
            "password_hash" to PasswordUtil.hash(password),
            "created_at" to System.currentTimeMillis()
        )
        val id = db.insert(DbHelper.TABLE_USERS, null, values)
        return if (id == -1L) null else id
    }

    /** Return userId kalau login sukses, null kalau username/password salah */
    fun loginUser(username: String, password: String): Long? {
        val db = helper.readableDatabase
        val cursor = db.query(
            DbHelper.TABLE_USERS, arrayOf("id", "password_hash"), "username=?",
            arrayOf(username), null, null, null
        )
        var result: Long? = null
        if (cursor.moveToFirst()) {
            val id = cursor.getLong(0)
            val storedHash = cursor.getString(1)
            if (PasswordUtil.hash(password) == storedHash) {
                result = id
            }
        }
        cursor.close()
        return result
    }

    // ---------- SESSION ----------

    fun createSession(userId: Long, title: String): Long {
        val db = helper.writableDatabase
        val values = cv(
            "user_id" to userId,
            "title" to title,
            "created_at" to System.currentTimeMillis()
        )
        return db.insert(DbHelper.TABLE_SESSIONS, null, values)
    }

    fun updateSessionTitle(sessionId: Long, title: String) {
        val db = helper.writableDatabase
        db.update(DbHelper.TABLE_SESSIONS, cv("title" to title), "id=?", arrayOf(sessionId.toString()))
    }

    fun deleteSession(sessionId: Long) {
        val db = helper.writableDatabase
        db.delete(DbHelper.TABLE_MESSAGES, "session_id=?", arrayOf(sessionId.toString()))
        db.delete(DbHelper.TABLE_SESSIONS, "id=?", arrayOf(sessionId.toString()))
    }

    fun getSessions(userId: Long): List<ChatSession> {
        val db = helper.readableDatabase
        val cursor = db.query(
            DbHelper.TABLE_SESSIONS, null, "user_id=?",
            arrayOf(userId.toString()), null, null, "created_at DESC"
        )
        val list = mutableListOf<ChatSession>()
        while (cursor.moveToNext()) {
            list.add(
                ChatSession(
                    id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                    userId = cursor.getLong(cursor.getColumnIndexOrThrow("user_id")),
                    title = cursor.getString(cursor.getColumnIndexOrThrow("title")),
                    createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("created_at"))
                )
            )
        }
        cursor.close()
        return list
    }

    // ---------- MESSAGE ----------

    fun addMessage(message: Message): Long {
        val db = helper.writableDatabase
        val values = cv(
            "session_id" to message.sessionId,
            "role" to message.role,
            "content" to message.content,
            "image_path" to message.imagePath,
            "timestamp" to message.timestamp
        )
        return db.insert(DbHelper.TABLE_MESSAGES, null, values)
    }

    fun updateMessageContent(messageId: Long, content: String) {
        val db = helper.writableDatabase
        db.update(DbHelper.TABLE_MESSAGES, cv("content" to content), "id=?", arrayOf(messageId.toString()))
    }

    fun getMessages(sessionId: Long): List<Message> {
        val db = helper.readableDatabase
        val cursor = db.query(
            DbHelper.TABLE_MESSAGES, null, "session_id=?",
            arrayOf(sessionId.toString()), null, null, "timestamp ASC"
        )
        val list = mutableListOf<Message>()
        while (cursor.moveToNext()) {
            list.add(
                Message(
                    id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                    sessionId = cursor.getLong(cursor.getColumnIndexOrThrow("session_id")),
                    role = cursor.getString(cursor.getColumnIndexOrThrow("role")),
                    content = cursor.getString(cursor.getColumnIndexOrThrow("content")),
                    imagePath = cursor.getString(cursor.getColumnIndexOrThrow("image_path")),
                    timestamp = cursor.getLong(cursor.getColumnIndexOrThrow("timestamp"))
                )
            )
        }
        cursor.close()
        return list
    }
}
