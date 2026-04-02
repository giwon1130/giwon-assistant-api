package com.giwon.assistant.features.briefing.service

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient

@Configuration
@EnableConfigurationProperties(
    AssistantWeatherProperties::class,
    AssistantCalendarProperties::class,
    AssistantNewsProperties::class,
    AssistantBriefingScheduleProperties::class,
)
class WeatherClientConfig {
    @Bean
    fun weatherRestClient(): RestClient =
        RestClient.builder()
            .baseUrl("https://api.open-meteo.com")
            .build()

    @Bean
    fun newsRestClient(): RestClient =
        RestClient.builder().build()
}
