package com.giwon.assistant.features.idea.service

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "assistant.openai")
data class AssistantOpenAiProperties(
    val model: String,
)
