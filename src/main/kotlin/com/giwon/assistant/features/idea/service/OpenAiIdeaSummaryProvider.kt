package com.giwon.assistant.features.idea.service

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.giwon.assistant.features.idea.dto.IdeaSummaryRequest
import com.giwon.assistant.features.idea.dto.IdeaSummaryResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

@Component
@ConditionalOnProperty(
    prefix = "assistant.integrations",
    name = ["openai-enabled"],
    havingValue = "true",
)
class OpenAiIdeaSummaryProvider(
    private val openAiRestClient: RestClient,
    private val openAiProperties: AssistantOpenAiProperties,
    @Value("\${OPENAI_API_KEY:}") private val openAiApiKey: String,
) : IdeaSummaryProvider {
    override fun summarize(request: IdeaSummaryRequest): IdeaSummaryResponse {
        require(openAiApiKey.isNotBlank()) { "OPENAI_API_KEY is required when assistant.integrations.openai-enabled=true" }

        val prompt = buildPrompt(request)
        val body = mapOf(
            "model" to openAiProperties.model,
            "input" to prompt,
        )

        val response = openAiRestClient.post()
            .uri("/v1/responses")
            .header("Authorization", "Bearer $openAiApiKey")
            .header("Content-Type", "application/json")
            .body(body)
            .retrieve()
            .body(OpenAiResponse::class.java)
            ?: error("OpenAI response is empty")

        val outputText = response.outputText?.takeIf { it.isNotBlank() }
            ?: error("OpenAI output_text is empty")

        return parseOutput(outputText, request)
    }

    private fun buildPrompt(request: IdeaSummaryRequest): String =
        """
        다음 아이디어를 제품 개발 관점에서 정리해줘.
        응답은 반드시 아래 형식을 지켜.

        TITLE: 한 줄 제목
        SUMMARY: 두세 문장 요약
        KEY_POINTS:
        - 포인트 1
        - 포인트 2
        - 포인트 3
        SUGGESTED_ACTIONS:
        - 액션 1
        - 액션 2
        - 액션 3

        원문 제목: ${request.title ?: "없음"}
        원문 내용:
        ${request.rawText}
        """.trimIndent()

    private fun parseOutput(outputText: String, request: IdeaSummaryRequest): IdeaSummaryResponse {
        val lines = outputText.lines().map { it.trim() }.filter { it.isNotBlank() }
        val title = lines.firstOrNull { it.startsWith("TITLE:") }
            ?.substringAfter("TITLE:")
            ?.trim()
            ?.ifBlank { request.title ?: "Untitled Idea" }
            ?: (request.title ?: "Untitled Idea")

        val summary = lines.firstOrNull { it.startsWith("SUMMARY:") }
            ?.substringAfter("SUMMARY:")
            ?.trim()
            ?.ifBlank { outputText.take(160) }
            ?: outputText.take(160)

        val keyPoints = extractBulletSection(lines, "KEY_POINTS:")
        val suggestedActions = extractBulletSection(lines, "SUGGESTED_ACTIONS:")

        return IdeaSummaryResponse(
            title = title,
            summary = summary,
            keyPoints = if (keyPoints.isNotEmpty()) keyPoints else listOf("핵심 포인트를 다시 확인할 필요가 있음"),
            suggestedActions = if (suggestedActions.isNotEmpty()) suggestedActions else listOf("다음 작업을 더 구체화할 필요가 있음"),
        )
    }

    private fun extractBulletSection(lines: List<String>, header: String): List<String> {
        val startIndex = lines.indexOfFirst { it == header }
        if (startIndex == -1) {
            return emptyList()
        }

        return lines.drop(startIndex + 1)
            .takeWhile { !it.endsWith(":") }
            .mapNotNull { line ->
                when {
                    line.startsWith("- ") -> line.removePrefix("- ").trim()
                    line.startsWith("* ") -> line.removePrefix("* ").trim()
                    else -> null
                }
            }
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
private data class OpenAiResponse(
    @JsonProperty("output_text")
    val outputText: String?,
)
