package com.giwon.assistant.features.briefing.service

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "assistant.briefing-schedule")
data class AssistantBriefingScheduleProperties(
    val enabled: Boolean = true,
    val cron: String = "0 0 8 * * *",
    val zone: String = "Asia/Seoul",
)
