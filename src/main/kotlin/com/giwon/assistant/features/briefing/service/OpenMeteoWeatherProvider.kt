package com.giwon.assistant.features.briefing.service

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.giwon.assistant.features.briefing.dto.WeatherSummary
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import kotlin.math.roundToInt

@Component
@ConditionalOnProperty(
    prefix = "assistant.integrations",
    name = ["weather-enabled"],
    havingValue = "true",
    matchIfMissing = true,
)
class OpenMeteoWeatherProvider(
    private val weatherRestClient: RestClient,
    private val weatherProperties: AssistantWeatherProperties,
) : WeatherProvider {
    override fun getCurrentWeather(): WeatherSummary {
        val response = weatherRestClient.get()
            .uri { builder ->
                builder.path("/v1/forecast")
                    .queryParam("latitude", weatherProperties.latitude)
                    .queryParam("longitude", weatherProperties.longitude)
                    .queryParam("current", "temperature_2m,weather_code")
                    .queryParam("timezone", "auto")
                    .build()
            }
            .retrieve()
            .body(OpenMeteoForecastResponse::class.java)
            ?: error("Open-Meteo response is empty")

        val current = response.current ?: error("Open-Meteo current weather is empty")

        return WeatherSummary(
            location = weatherProperties.locationName,
            condition = mapWeatherCode(current.weatherCode),
            temperatureCelsius = current.temperature.roundToInt(),
        )
    }

    private fun mapWeatherCode(code: Int): String =
        when (code) {
            0 -> "맑음"
            1, 2, 3 -> "대체로 맑음"
            45, 48 -> "안개"
            51, 53, 55, 56, 57 -> "이슬비"
            61, 63, 65, 66, 67 -> "비"
            71, 73, 75, 77 -> "눈"
            80, 81, 82 -> "소나기"
            85, 86 -> "눈 소나기"
            95, 96, 99 -> "뇌우"
            else -> "날씨 정보 확인 필요"
        }
}

@JsonIgnoreProperties(ignoreUnknown = true)
private data class OpenMeteoForecastResponse(
    val current: OpenMeteoCurrentWeather?,
)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class OpenMeteoCurrentWeather(
    @JsonProperty("temperature_2m")
    val temperature: Double,
    @JsonProperty("weather_code")
    val weatherCode: Int,
)
