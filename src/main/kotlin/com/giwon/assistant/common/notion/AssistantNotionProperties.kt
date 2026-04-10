package com.giwon.assistant.common.notion

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "assistant.notion")
data class AssistantNotionProperties(
    val enabled: Boolean = false,
    val token: String = "",
    val briefingDatabaseId: String = "",
    val ideaDatabaseId: String = "",
)
