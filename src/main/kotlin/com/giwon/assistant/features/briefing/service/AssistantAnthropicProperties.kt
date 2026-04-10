package com.giwon.assistant.features.briefing.service

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "assistant.anthropic")
data class AssistantAnthropicProperties(
    val model: String = "claude-sonnet-4-5",
)
