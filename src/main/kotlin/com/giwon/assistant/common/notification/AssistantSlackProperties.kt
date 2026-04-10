package com.giwon.assistant.common.notification

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "assistant.slack")
data class AssistantSlackProperties(
    val enabled: Boolean = false,
    val webhookUrl: String = "",
)
