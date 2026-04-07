package com.giwon.assistant.features.idea.service

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "assistant.gemini")
data class AssistantGeminiProperties(
    val model: String = "gemini-2.0-flash",
)
