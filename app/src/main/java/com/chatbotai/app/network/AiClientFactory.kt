package com.chatbotai.app.network

import com.chatbotai.app.util.Prefs

object AiClientFactory {
    fun get(provider: String): AiStreamClient = when (provider) {
        Prefs.PROVIDER_OPENAI -> OpenAiClient
        Prefs.PROVIDER_CLAUDE -> ClaudeClient
        else -> GeminiClient
    }
}
