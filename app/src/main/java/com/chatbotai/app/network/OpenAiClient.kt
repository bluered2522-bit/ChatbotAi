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

object OpenAiClient : AiStreamClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private const val URL = "https://api.openai.com/v1/chat/completions"

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
            callback.onError("OpenAI API Key belum diisi. Buka Pengaturan dulu ya.")
            return
        }

        val messages = JSONArray()
        for (m in history) {
            val role = if (m.role == ROLE_USER) "user" else "assistant"
            messages.put(JSONObject().put("role", role).put("content", m.content))
        }

        // Pesan baru — kalau ada gambar, content-nya array (multimodal)
        if (!imageBase64.isNullOrBlank() && !imageMimeType.isNullOrBlank()) {
            val contentArr = JSONArray()
            contentArr.put(JSONObject().put("type", "text").put("text", newUserText))
            val imageUrl = JSONObject().put("url", "data:$imageMimeType;base64,$imageBase64")
            contentArr.put(JSONObject().put("type", "image_url").put("image_url", imageUrl))
            messages.put(JSONObject().put("role", "user").put("content", contentArr))
        } else {
            messages.put(JSONObject().put("role", "user").put("content", newUserText))
        }

        val bodyJson = JSONObject()
            .put("model", model)
            .put("stream", true)
            .put("messages", messages)

        val request = Request.Builder()
            .url(URL)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
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
                        if (jsonStr.isEmpty() || jsonStr == "[DONE]") continue
                        try {
                            val json = JSONObject(jsonStr)
                            val choices = json.optJSONArray("choices")
                            if (choices != null && choices.length() > 0) {
                                val delta = choices.getJSONObject(0).optJSONObject("delta")
                                val text = delta?.optString("content", "") ?: ""
                                if (text.isNotEmpty()) {
                                    fullText.append(text)
                                    callback.onChunk(text)
                                }
                            }
                        } catch (e: Exception) {
                            Log.w("OpenAiClient", "Gagal parse chunk: $jsonStr", e)
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
