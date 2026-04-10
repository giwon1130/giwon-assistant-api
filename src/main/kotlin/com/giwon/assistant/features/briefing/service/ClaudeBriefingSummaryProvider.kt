package com.giwon.assistant.features.briefing.service

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

@Component
@ConditionalOnProperty(
    prefix = "assistant.integrations",
    name = ["claude-enabled"],
    havingValue = "true",
)
class ClaudeBriefingSummaryProvider(
    @Qualifier("claudeRestClient") private val claudeRestClient: RestClient,
    private val anthropicProperties: AssistantAnthropicProperties,
    @Value("\${ANTHROPIC_API_KEY:}") private val apiKey: String,
) : BriefingSummaryProvider {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun summarize(request: BriefingSummaryRequest): BriefingSummaryResult {
        require(apiKey.isNotBlank()) { "ANTHROPIC_API_KEY is required when assistant.integrations.claude-enabled=true" }

        val prompt = buildPrompt(request)
        val body = mapOf(
            "model" to anthropicProperties.model,
            "max_tokens" to 512,
            "messages" to listOf(mapOf("role" to "user", "content" to prompt)),
        )

        val response = claudeRestClient.post()
            .uri("/v1/messages")
            .header("x-api-key", apiKey)
            .header("anthropic-version", "2023-06-01")
            .body(body)
            .retrieve()
            .body(ClaudeMessagesResponse::class.java)
            ?: error("Claude response is empty")

        val outputText = response.content
            ?.firstOrNull { it.type == "text" }
            ?.text
            ?.takeIf { it.isNotBlank() }
            ?: error("Claude content is empty")

        return parseOutput(outputText)
    }

    private fun buildPrompt(request: BriefingSummaryRequest): String {
        val calendarText = request.calendar.joinToString("\n") { "- ${it.time} ${it.title}" }
        val headlinesText = request.headlines.joinToString("\n") { "- [${it.source}] ${it.title}" }
        val tasksText = request.tasks.joinToString("\n") { "- [${it.priority}] ${it.title}" }
        val weatherText = "${request.weather.condition} ${request.weather.temperatureCelsius}°C (${request.weather.location})"

        return """
        오늘 하루 브리핑을 생성해줘. 반드시 아래 형식만 사용해.

        SUMMARY: 오늘 하루 전체 흐름을 2~3문장으로 요약
        FOCUS: 오늘 집중해야 할 방향을 1~2문장으로 제안

        오늘 날씨: $weatherText
        오늘 일정:
        $calendarText
        주요 뉴스:
        $headlinesText
        할 일:
        $tasksText
        """.trimIndent()
    }

    private fun parseOutput(text: String): BriefingSummaryResult {
        val lines = text.lines().map { it.trim() }.filter { it.isNotBlank() }
        val summary = lines.firstOrNull { it.startsWith("SUMMARY:") }
            ?.substringAfter("SUMMARY:")?.trim()
            ?: text.take(200)
        val focus = lines.firstOrNull { it.startsWith("FOCUS:") }
            ?.substringAfter("FOCUS:")?.trim()
            ?: "오늘 가장 중요한 작업에 먼저 집중하세요."

        return BriefingSummaryResult(summary = summary, focusSuggestion = focus)
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
private data class ClaudeMessagesResponse(
    val content: List<ClaudeContentBlock>? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class ClaudeContentBlock(
    val type: String? = null,
    val text: String? = null,
)
