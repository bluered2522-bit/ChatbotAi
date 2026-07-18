package com.chatbotai.app.network

import android.util.Log
import com.chatbotai.app.model.Message
import com.chatbotai.app.model.ROLE_USER
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

object ClaudeClient : AiStreamClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private const val URL = "https://api.anthropic.com/v1/messages"
    private const val ANTHROPIC_VERSION = "2023-06-01"

    override fun sendMessageStream(
        apiKey: String,
        model: String,
        history: List<Message>,
        newUserText: String,
        imageBase64: String?,
        imageMimeType: String?,
        callback: AiStreamClient.StreamCallback
    ) {
        if (apiKey.isBlank()) {
            callback.onError("Claude API Key belum diisi. Buka Pengaturan dulu ya.")
            return
        }

        val messages = JSONArray()
        for (m in history) {
            val role = if (m.role == ROLE_USER) "user" else "assistant"
            messages.put(JSONObject().put("role", role).put("content", m.content))
        }

        if (!imageBase64.isNullOrBlank() && !imageMimeType.isNullOrBlank()) {
            val contentArr = JSONArray()
            val source = JSONObject()
                .put("type", "base64")
                .put("media_type", imageMimeType)
                .put("data", imageBase64)
            contentArr.put(JSONObject().put("type", "image").put("source", source))
            contentArr.put(JSONObject().put("type", "text").put("text", newUserText))
            messages.put(JSONObject().put("role", "user").put("content", contentArr))
        } else {
            messages.put(JSONObject().put("role", "user").put("content", newUserText))
        }

        val bodyJson = JSONObject()
            .put("model", model)
            .put("max_tokens", 4096)
            .put("stream", true)
            .put("messages", messages)

        val request = Request.Builder()
            .url(URL)
            .addHeader("x-api-key", apiKey)
            .addHeader("anthropic-version", ANTHROPIC_VERSION)
            .addHeader("content-type", "application/json")
            .post(bodyJson.toString().toRequestBody("application/json".toMediaType()))
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errBody = response.body?.string().orEmpty()
                    if (response.code == 401) {
                        callback.onError(ERROR_API_MISMATCH)
                    } else {
                        callback.onError("Error ${response.code}: ${extractErrorMessage(errBody)}")
                    }
                    return
                }

                val source = response.body?.source()
                if (source == null) {
                    callback.onError("Response kosong dari server")
                    return
                }

                val fullText = StringBuilder()
                while (!source.exhausted()) {
                    val line = source.readUtf8Line() ?: break
                    if (line.startsWith("data:")) {
                        val jsonStr = line.removePrefix("data:").trim()
                        if (jsonStr.isEmpty()) continue
                        try {
                            val json = JSONObject(jsonStr)
                            val type = json.optString("type")
                            if (type == "content_block_delta") {
                                val delta = json.optJSONObject("delta")
                                if (delta != null && delta.optString("type") == "text_delta") {
                                    val text = delta.optString("text", "")
                                    if (text.isNotEmpty()) {
                                        fullText.append(text)
                                        callback.onChunk(text)
                                    }
                                }
                            } else if (type == "error") {
                                val errMsg = json.optJSONObject("error")?.optString("message") ?: "Unknown error"
                                callback.onError(errMsg)
                                return
                            }
                        } catch (e: Exception) {
                            Log.w("ClaudeClient", "Gagal parse chunk: $jsonStr", e)
                        }
                    }
                }
                callback.onComplete(fullText.toString())
            }
        } catch (e: IOException) {
            callback.onError("Koneksi gagal: ${e.message}")
        } catch (e: Exception) {
            callback.onError("Terjadi kesalahan: ${e.message}")
        }
    }

    private fun extractErrorMessage(body: String): String {
        return try {
            val json = JSONObject(body)
            json.optJSONObject("error")?.optString("message") ?: body.take(200)
        } catch (e: Exception) {
            body.take(200)
        }
    }
}
