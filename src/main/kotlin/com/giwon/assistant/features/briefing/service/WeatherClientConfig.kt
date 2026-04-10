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
    AssistantAnthropicProperties::class,
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

    @Bean
    fun googleCalendarRestClient(): RestClient =
        RestClient.builder()
            .baseUrl("https://www.googleapis.com")
            .build()

    @Bean
    fun googleOAuthRestClient(): RestClient =
        RestClient.builder()
            .baseUrl("https://oauth2.googleapis.com")
            .build()

    @Bean
    fun claudeRestClient(): RestClient =
        RestClient.builder()
            .baseUrl("https://api.anthropic.com")
            .build()
}
