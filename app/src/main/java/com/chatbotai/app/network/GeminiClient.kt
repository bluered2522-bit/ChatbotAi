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

object GeminiClient : AiStreamClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"

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
            callback.onError("Gemini API Key belum diisi. Buka Pengaturan dulu ya.")
            return
        }

        val contents = JSONArray()
        for (m in history) {
            val role = if (m.role == ROLE_USER) "user" else "model"
            val parts = JSONArray()
            parts.put(JSONObject().put("text", m.content))
            contents.put(JSONObject().put("role", role).put("parts", parts))
        }

        val newParts = JSONArray()
        if (!imageBase64.isNullOrBlank() && !imageMimeType.isNullOrBlank()) {
            val inlineData = JSONObject()
                .put("mimeType", imageMimeType)
                .put("data", imageBase64)
            newParts.put(JSONObject().put("inlineData", inlineData))
        }
        newParts.put(JSONObject().put("text", newUserText))
        contents.put(JSONObject().put("role", "user").put("parts", newParts))

        val requestBodyJson = JSONObject().put("contents", contents)

        val url = "$BASE_URL/$model:streamGenerateContent?alt=sse"

        val request = Request.Builder()
            .url(url)
            .addHeader("x-goog-api-key", apiKey)
            .addHeader("Content-Type", "application/json")
            .post(requestBodyJson.toString().toRequestBody("application/json".toMediaType()))
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errBody = response.body?.string().orEmpty()
                    if (response.code == 400 && errBody.contains("API_KEY_INVALID")) {
                        callback.onError(ERROR_API_MISMATCH)
                    } else if (response.code == 403) {
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
                            val candidates = json.optJSONArray("candidates")
                            if (candidates != null && candidates.length() > 0) {
                                val content = candidates.getJSONObject(0).optJSONObject("content")
                                val partsArr = content?.optJSONArray("parts")
                                if (partsArr != null) {
                                    for (i in 0 until partsArr.length()) {
                                        val text = partsArr.getJSONObject(i).optString("text", "")
                                        if (text.isNotEmpty()) {
                                            fullText.append(text)
                                            callback.onChunk(text)
                                        }
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Log.w("GeminiClient", "Gagal parse chunk: $jsonStr", e)
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
