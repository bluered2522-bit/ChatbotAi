package com.chatbotai.app.ui

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chatbotai.app.R
import com.chatbotai.app.db.ChatRepository
import com.chatbotai.app.model.Message
import com.chatbotai.app.model.ROLE_MODEL
import com.chatbotai.app.model.ROLE_USER
import com.chatbotai.app.network.AiClientFactory
import com.chatbotai.app.network.AiStreamClient
import com.chatbotai.app.ui.adapter.MessageAdapter
import com.chatbotai.app.util.Prefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

class ChatActivity : AppCompatActivity() {

    private lateinit var repository: ChatRepository
    private lateinit var prefs: Prefs
    private lateinit var adapter: MessageAdapter
    private lateinit var rvMessages: RecyclerView
    private lateinit var etMessage: EditText
    private lateinit var layoutAttachmentPreview: LinearLayout
    private lateinit var ivAttachmentPreview: ImageView

    private var sessionId: Long = -1L
    private var selectedImageUri: Uri? = null

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            selectedImageUri = uri
            layoutAttachmentPreview.visibility = LinearLayout.VISIBLE
            ivAttachmentPreview.setImageURI(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        repository = ChatRepository(this)
        prefs = Prefs(this)

        if (prefs.loggedInUserId == -1L) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        rvMessages = findViewById(R.id.rvMessages)
        etMessage = findViewById(R.id.etMessage)
        layoutAttachmentPreview = findViewById(R.id.layoutAttachmentPreview)
        ivAttachmentPreview = findViewById(R.id.ivAttachmentPreview)
        val btnAttach = findViewById<ImageButton>(R.id.btnAttach)
        val btnRemoveAttachment = findViewById<ImageButton>(R.id.btnRemoveAttachment)
        val btnSend = findViewById<ImageButton>(R.id.btnSend)

        adapter = MessageAdapter(mutableListOf())
        rvMessages.layoutManager = LinearLayoutManager(this)
        rvMessages.adapter = adapter

        sessionId = intent.getLongExtra(EXTRA_SESSION_ID, -1L)
        if (sessionId != -1L) {
            loadMessages(sessionId)
        }

        btnAttach.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        btnRemoveAttachment.setOnClickListener {
            selectedImageUri = null
            layoutAttachmentPreview.visibility = LinearLayout.GONE
        }

        btnSend.setOnClickListener {
            sendMessage()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_chat, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_new_chat -> startNewChat()
            R.id.action_history -> startActivity(Intent(this, HistoryActivity::class.java))
            R.id.action_settings -> startActivity(Intent(this, SettingsActivity::class.java))
            R.id.action_logout -> logout()
        }
        return true
    }

    private fun startNewChat() {
        sessionId = -1L
        prefs.lastSessionId = -1L
        adapter.clearAll()
    }

    private fun logout() {
        prefs.clearSession()
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    private fun loadMessages(id: Long) {
        val messages = repository.getMessages(id)
        adapter.submitInitial(messages)
        if (adapter.itemCount > 0) rvMessages.scrollToPosition(adapter.itemCount - 1)
    }

    private fun sendMessage() {
        val text = etMessage.text.toString().trim()
        if (text.isEmpty() && selectedImageUri == null) return

        val apiKey = prefs.apiKey
        if (apiKey.isBlank()) {
            Toast.makeText(this, "Isi dulu API Key di Pengaturan", Toast.LENGTH_LONG).show()
            return
        }

        // Buat sesi baru kalau ini pesan pertama di chat ini
        if (sessionId == -1L) {
            val title = if (text.isNotEmpty()) {
                if (text.length > 30) text.take(30) + "…" else text
            } else "Chat Gambar"
            sessionId = repository.createSession(prefs.loggedInUserId, title)
            prefs.lastSessionId = sessionId
        }

        val historyBeforeThisMessage = repository.getMessages(sessionId)
            .filter { it.role == ROLE_USER || it.role == ROLE_MODEL }

        // Simpan gambar (kalau ada) ke internal storage biar persist selamanya
        var imagePath: String? = null
        var imageBase64: String? = null
        var imageMime: String? = null
        val uri = selectedImageUri
        if (uri != null) {
            val result = saveImageLocally(uri)
            imagePath = result?.first
            imageBase64 = result?.second
            imageMime = "image/jpeg"
        }

        val userMessage = Message(sessionId = sessionId, role = ROLE_USER, content = text, imagePath = imagePath)
        val userMessageId = repository.addMessage(userMessage)
        adapter.addMessage(userMessage.copy(id = userMessageId))

        etMessage.text.clear()
        selectedImageUri = null
        layoutAttachmentPreview.visibility = LinearLayout.GONE
        rvMessages.scrollToPosition(adapter.itemCount - 1)

        adapter.showTyping()
        rvMessages.scrollToPosition(adapter.itemCount - 1)

        val provider = prefs.apiProvider
        val model = prefs.getModelFor(provider)
        val client = AiClientFactory.get(provider)
        val currentSessionId = sessionId

        lifecycleScope.launch(Dispatchers.IO) {
            var aiMessageStarted = false
            val accumulated = StringBuilder()

            client.sendMessageStream(
                apiKey = apiKey,
                model = model,
                history = historyBeforeThisMessage,
                newUserText = text.ifEmpty { "Tolong deskripsikan gambar ini." },
                imageBase64 = imageBase64,
                imageMimeType = imageMime,
                callback = object : AiStreamClient.StreamCallback {
                    override fun onChunk(textDelta: String) {
                        accumulated.append(textDelta)
                        runOnUiThread {
                            if (!aiMessageStarted) {
                                aiMessageStarted = true
                                adapter.removeTyping()
                                adapter.addMessage(
                                    Message(sessionId = currentSessionId, role = ROLE_MODEL, content = accumulated.toString())
                                )
                            } else {
                                adapter.updateLastMessageContent(accumulated.toString())
                            }
                            rvMessages.scrollToPosition(adapter.itemCount - 1)
                        }
                    }

                    override fun onComplete(fullText: String) {
                        val finalText = fullText.ifBlank { "(Tidak ada respons dari AI)" }
                        runOnUiThread {
                            if (!aiMessageStarted) {
                                adapter.removeTyping()
                                adapter.addMessage(Message(sessionId = currentSessionId, role = ROLE_MODEL, content = finalText))
                            }
                        }
                        if (fullText.isNotBlank()) {
                            repository.addMessage(Message(sessionId = currentSessionId, role = ROLE_MODEL, content = fullText))
                        }
                    }

                    override fun onError(message: String) {
                        runOnUiThread {
                            adapter.removeTyping()
                            adapter.addMessage(Message(sessionId = currentSessionId, role = ROLE_MODEL, content = "⚠️ $message"))
                            rvMessages.scrollToPosition(adapter.itemCount - 1)
                        }
                    }
                }
            )
        }
    }

    /** Simpan gambar terpilih ke internal storage. Return Pair(absolutePath, base64) */
    private fun saveImageLocally(uri: Uri): Pair<String, String>? {
        return try {
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            val dir = File(filesDir, "attachments")
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, "img_${System.currentTimeMillis()}.jpg")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
            }

            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos)
            val base64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)

            Pair(file.absolutePath, base64)
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        const val EXTRA_SESSION_ID = "extra_session_id"
    }
}
