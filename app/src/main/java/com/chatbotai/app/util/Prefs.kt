package com.chatbotai.app.util

import android.content.Context

class Prefs(context: Context) {
    private val sp = context.applicationContext.getSharedPreferences("chatbotai_prefs", Context.MODE_PRIVATE)

    var loggedInUserId: Long
        get() = sp.getLong(KEY_USER_ID, -1L)
        set(value) = sp.edit().putLong(KEY_USER_ID, value).apply()

    var loggedInUsername: String
        get() = sp.getString(KEY_USERNAME, "") ?: ""
        set(value) = sp.edit().putString(KEY_USERNAME, value).apply()

    /** Satu API key yang dipakai, sesuai provider yang lagi aktif */
    var apiKey: String
        get() = sp.getString(KEY_API_KEY, "") ?: ""
        set(value) = sp.edit().putString(KEY_API_KEY, value).apply()

    /** "gemini" | "openai" | "claude" */
    var apiProvider: String
        get() = sp.getString(KEY_PROVIDER, PROVIDER_GEMINI) ?: PROVIDER_GEMINI
        set(value) = sp.edit().putString(KEY_PROVIDER, value).apply()

    /** Nama model per-provider, disimpan terpisah biar gak ketimpa pas ganti-ganti provider */
    fun getModelFor(provider: String): String {
        val key = KEY_MODEL_PREFIX + provider
        return sp.getString(key, defaultModelFor(provider)) ?: defaultModelFor(provider)
    }

    fun setModelFor(provider: String, model: String) {
        sp.edit().putString(KEY_MODEL_PREFIX + provider, model).apply()
    }

    var lastSessionId: Long
        get() = sp.getLong(KEY_LAST_SESSION, -1L)
        set(value) = sp.edit().putLong(KEY_LAST_SESSION, value).apply()

    fun clearSession() {
        sp.edit().remove(KEY_USER_ID).remove(KEY_USERNAME).remove(KEY_LAST_SESSION).apply()
    }

    companion object {
        private const val KEY_USER_ID = "logged_in_user_id"
        private const val KEY_USERNAME = "logged_in_username"
        private const val KEY_API_KEY = "api_key"
        private const val KEY_PROVIDER = "api_provider"
        private const val KEY_MODEL_PREFIX = "model_"
        private const val KEY_LAST_SESSION = "last_session_id"

        const val PROVIDER_GEMINI = "gemini"
        const val PROVIDER_OPENAI = "openai"
        const val PROVIDER_CLAUDE = "claude"

        fun defaultModelFor(provider: String): String = when (provider) {
            PROVIDER_GEMINI -> "gemini-3.5-flash"
            PROVIDER_OPENAI -> "gpt-5.6"
            PROVIDER_CLAUDE -> "claude-sonnet-5"
            else -> "gemini-3.5-flash"
        }

        fun labelFor(provider: String): String = when (provider) {
            PROVIDER_GEMINI -> "Google Gemini"
            PROVIDER_OPENAI -> "OpenAI"
            PROVIDER_CLAUDE -> "Anthropic Claude"
            else -> provider
        }
    }
}
