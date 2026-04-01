package com.giwon.assistant.features.briefing.service

import com.giwon.assistant.features.briefing.dto.WeatherSummary
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(
    prefix = "assistant.integrations",
    name = ["weather-enabled"],
    havingValue = "false",
)
class MockWeatherProvider(
    private val weatherProperties: AssistantWeatherProperties,
) : WeatherProvider {
    override fun getCurrentWeather(): WeatherSummary =
        WeatherSummary(
            location = weatherProperties.locationName,
            condition = "맑음",
            temperatureCelsius = 18,
        )
}
