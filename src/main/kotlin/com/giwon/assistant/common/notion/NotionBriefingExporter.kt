package com.giwon.assistant.common.notion

import com.giwon.assistant.features.briefing.dto.TodayBriefingResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

@Component
class NotionBriefingExporter(
    private val notionRestClient: RestClient,
    private val properties: AssistantNotionProperties,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun export(briefing: TodayBriefingResponse, source: String) {
        if (!properties.enabled || properties.briefingDatabaseId.isBlank()) return

        val date = runCatching {
            briefing.generatedAt.substring(0, 10)
        }.getOrElse { briefing.generatedAt }

        val weatherText = "${briefing.weather.condition} ${briefing.weather.temperatureCelsius}°C (${briefing.weather.location})"

        val body = mapOf(
            "parent" to mapOf("database_id" to properties.briefingDatabaseId),
            "properties" to mapOf(
                "날짜" to mapOf("title" to listOf(mapOf("text" to mapOf("content" to date)))),
                "요약" to mapOf("rich_text" to listOf(mapOf("text" to mapOf("content" to briefing.summary)))),
                "날씨" to mapOf("rich_text" to listOf(mapOf("text" to mapOf("content" to weatherText)))),
                "집중_제안" to mapOf("rich_text" to listOf(mapOf("text" to mapOf("content" to briefing.focusSuggestion)))),
                "출처" to mapOf("select" to mapOf("name" to source)),
            ),
        )

        runCatching {
            notionRestClient.post()
                .uri("/pages")
                .body(body)
                .retrieve()
                .toBodilessEntity()
            log.info("Briefing exported to Notion: $date")
        }.onFailure {
            log.warn("Failed to export briefing to Notion: ${it.message}")
        }
    }
}
