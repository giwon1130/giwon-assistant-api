package com.giwon.assistant.features.briefing.service

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "assistant.calendar")
data class AssistantCalendarProperties(
    val timezone: String,
    val defaultEvents: List<AssistantCalendarEventProperty>,
    val calendarId: String = "primary",
    val clientId: String = "",
    val clientSecret: String = "",
    val refreshToken: String = "",
)

data class AssistantCalendarEventProperty(
    val time: String,
    val title: String,
)
