package com.giwon.assistant.common.notification

import com.giwon.assistant.features.briefing.dto.TodayBriefingResponse
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

@Configuration
@EnableConfigurationProperties(AssistantSlackProperties::class)
class SlackNotifierConfig {
    @Bean
    fun slackRestClient(): RestClient = RestClient.builder().build()
}

@Component
class SlackNotifier(
    private val slackRestClient: RestClient,
    private val properties: AssistantSlackProperties,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun sendBriefing(briefing: TodayBriefingResponse) {
        if (!properties.enabled || properties.webhookUrl.isBlank()) return

        val calendarText = briefing.calendar.joinToString("\n") { "• ${it.time} ${it.title}${if (it.mock) " _(목데이터)_" else ""}" }
        val headlinesText = briefing.headlines.take(3).joinToString("\n") { "• [${it.source}] ${it.title}${if (it.mock) " _(목데이터)_" else ""}" }
        val tasksText = briefing.tasks.joinToString("\n") { "• `${it.priority}` ${it.title}${if (it.mock) " _(목데이터)_" else ""}" }

        val text = """
            *🌅 오늘의 브리핑*
            > ${briefing.summary}

            *🌤 날씨*
            ${briefing.weather.condition} ${briefing.weather.temperatureCelsius}°C (${briefing.weather.location})

            *📅 오늘 일정*
            $calendarText

            *📰 주요 뉴스*
            $headlinesText

            *✅ 할 일*
            $tasksText

            *🎯 집중 제안*
            > ${briefing.focusSuggestion}
        """.trimIndent()

        val body = mapOf("text" to text)

        runCatching {
            slackRestClient.post()
                .uri(properties.webhookUrl)
                .body(body)
                .retrieve()
                .toBodilessEntity()
            log.info("Briefing sent to Slack")
        }.onFailure {
            log.warn("Failed to send briefing to Slack: ${it.message}")
        }
    }
}
