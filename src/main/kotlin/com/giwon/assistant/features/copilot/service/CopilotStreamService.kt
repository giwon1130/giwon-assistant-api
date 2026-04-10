package com.giwon.assistant.features.copilot.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.giwon.assistant.features.briefing.service.AssistantAnthropicProperties
import com.giwon.assistant.features.idea.service.IdeaService
import com.giwon.assistant.features.planner.service.PlannerService
import com.giwon.assistant.features.copilot.repository.CopilotHistoryRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.util.concurrent.Executors

@Service
class CopilotStreamService(
    private val copilotService: CopilotService,
    private val ideaService: IdeaService,
    private val plannerService: PlannerService,
    private val copilotHistoryRepository: CopilotHistoryRepository,
    @Qualifier("claudeRestClient") private val claudeRestClient: RestClient,
    private val anthropicProperties: AssistantAnthropicProperties,
    private val objectMapper: ObjectMapper,
    @Value("\${assistant.integrations.claude-enabled:false}") private val claudeEnabled: Boolean,
    @Value("\${ANTHROPIC_API_KEY:}") private val anthropicApiKey: String,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val executor = Executors.newCachedThreadPool()

    fun askStream(question: String): SseEmitter {
        val emitter = SseEmitter(60_000L)

        executor.submit {
            runCatching {
                if (!claudeEnabled || anthropicApiKey.isBlank()) {
                    // fallback: non-streaming
                    val response = copilotService.ask(question)
                    emitter.send(SseEmitter.event().name("token").data(response.answer))
                    emitter.send(SseEmitter.event().name("done").data(""))
                    emitter.complete()
                    return@runCatching
                }

                val copilot = copilotService.getTodayCopilot()
                val ideas = ideaService.getAll().take(3)
                val recentHistory = copilotHistoryRepository.findTop10ByOrderByGeneratedAtDesc()
                    .reversed().takeLast(3)

                val prompt = buildStreamPrompt(question, copilot, ideas, recentHistory)

                val body = mapOf(
                    "model" to anthropicProperties.model,
                    "max_tokens" to 512,
                    "stream" to true,
                    "messages" to listOf(mapOf("role" to "user", "content" to prompt)),
                )

                claudeRestClient.post()
                    .uri("/v1/messages")
                    .header("x-api-key", anthropicApiKey)
                    .header("anthropic-version", "2023-06-01")
                    .body(body)
                    .exchange { _, response ->
                        val stream: InputStream = response.body
                        val reader = BufferedReader(InputStreamReader(stream))
                        reader.lines().forEach { line ->
                            if (line.startsWith("data: ")) {
                                val json = line.removePrefix("data: ").trim()
                                if (json == "[DONE]") return@forEach
                                runCatching {
                                    val node = objectMapper.readTree(json)
                                    if (node.path("type").asText() == "content_block_delta") {
                                        val text = node.path("delta").path("text").asText()
                                        if (text.isNotEmpty()) {
                                            emitter.send(SseEmitter.event().name("token").data(text))
                                        }
                                    }
                                }
                            }
                        }
                        emitter.send(SseEmitter.event().name("done").data(""))
                        emitter.complete()
                    }
            }.onFailure { e ->
                log.error("SSE stream error: ${e.message}")
                runCatching { emitter.completeWithError(e) }
            }
        }

        return emitter
    }

    private fun buildStreamPrompt(
        question: String,
        copilot: com.giwon.assistant.features.copilot.dto.TodayCopilotResponse,
        ideas: List<com.giwon.assistant.features.idea.dto.IdeaDetailResponse>,
        recentHistory: List<com.giwon.assistant.features.copilot.entity.CopilotHistoryEntity>,
    ): String {
        val historySection = if (recentHistory.isNotEmpty()) {
            "\n이전 대화:\n" + recentHistory.joinToString("\n") { "Q: ${it.question}\nA: ${it.answer}" }
        } else ""

        return """
        너는 개인 생산성 코파일럿이다. 질문에 자연스럽고 짧게 답해줘. 형식 없이 바로 답변만 써줘.

        오늘 헤드라인: ${copilot.headline}
        오늘 우선순위: ${copilot.topPriority}
        최근 아이디어: ${ideas.joinToString(", ") { it.title }}
        $historySection

        질문: $question
        """.trimIndent()
    }
}
