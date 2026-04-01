package com.giwon.assistant.features.briefing.service

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "assistant.weather")
data class AssistantWeatherProperties(
    val locationName: String,
    val latitude: Double,
    val longitude: Double,
)
