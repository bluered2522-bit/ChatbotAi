package com.chatbotai.app.network

import com.chatbotai.app.model.Message

/**
 * Interface umum, semua provider (Gemini/OpenAI/Claude) implementasi ini
 * biar ChatActivity gak perlu tau detail masing-masing provider.
 */
interface AiStreamClient {

    interface StreamCallback {
        fun onChunk(textDelta: String)
        fun onComplete(fullText: String)
        fun onError(message: String)
    }

    /**
     * FUNGSI INI BLOCKING — panggil dari background thread/coroutine.
     * history = pesan-pesan sebelumnya di sesi ini (belum termasuk pesan baru).
     */
    fun sendMessageStream(
        apiKey: String,
        model: String,
        history: List<Message>,
        newUserText: String,
        imageBase64: String?,
        imageMimeType: String?,
        callback: StreamCallback
    )
}

/** Pesan error generik yang dipakai semua client kalau kunci API kedeteksi salah/gak cocok */
const val ERROR_API_MISMATCH = "API tidak cocok dengan model!"
