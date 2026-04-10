package com.giwon.assistant.features.copilot.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.giwon.assistant.features.briefing.service.AssistantAnthropicProperties
import com.giwon.assistant.features.copilot.dto.CopilotAskResponse
import com.giwon.assistant.features.copilot.dto.CopilotSuggestedActionPlan
import com.giwon.assistant.features.copilot.dto.TodayCopilotResponse
import com.giwon.assistant.features.copilot.entity.CopilotHistoryEntity
import com.giwon.assistant.features.idea.service.AssistantGeminiProperties
import com.giwon.assistant.features.idea.service.AssistantOpenAiProperties
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException
import java.time.OffsetDateTime
import java.time.ZoneOffset

/**
 * LLM 제공자(Gemini, Claude, OpenAI)를 통한 코파일럿 질문 처리.
 * 프롬프트 빌드, 응답 파싱, 인텐트 감지, 액션 플랜 생성 담당.
 */
@Service
class CopilotLlmService(
    @Qualifier("geminiRestClient") private val geminiRestClient: RestClient,
    @Qualifier("claudeRestClient") private val claudeRestClient: RestClient,
    @Qualifier("openAiRestClient") private val openAiRestClient: RestClient,
    private val geminiProperties: AssistantGeminiProperties,
    private val openAiProperties: AssistantOpenAiProperties,
    private val anthropicProperties: AssistantAnthropicProperties,
    private val objectMapper: ObjectMapper,
    @Value("\${assistant.integrations.gemini-enabled:false}") private val geminiEnabled: Boolean,
    @Value("\${assistant.integrations.openai-enabled:false}") private val openAiEnabled: Boolean,
    @Value("\${assistant.integrations.claude-enabled:false}") private val claudeEnabled: Boolean,
    @Value("\${GEMINI_API_KEY:}") private val geminiApiKey: String,
    @Value("\${OPENAI_API_KEY:}") private val openAiApiKey: String,
    @Value("\${ANTHROPIC_API_KEY:}") private val anthropicApiKey: String,
) {
    fun askWithGemini(
        question: String,
        copilot: TodayCopilotResponse,
        ideas: List<com.giwon.assistant.features.idea.dto.IdeaDetailResponse>,
        recentHistory: List<CopilotHistoryEntity>,
        goodExamples: List<CopilotHistoryEntity> = emptyList(),
        badExamples: List<CopilotHistoryEntity> = emptyList(),
        providerErrors: MutableList<String>,
    ): CopilotAskResponse? {
        if (!geminiEnabled || geminiApiKey.isBlank()) return null

        return runCatching {
            val prompt = buildAskPrompt(question, copilot, ideas, recentHistory, goodExamples, badExamples)
            val body = mapOf(
                "contents" to listOf(
                    mapOf("parts" to listOf(mapOf("text" to prompt)))
                )
            )
            val responseBody = geminiRestClient.post()
                .uri { it.path("/v1beta/models/{model}:generateContent").queryParam("key", geminiApiKey).build(geminiProperties.model) }
                .header("Content-Type", "application/json")
                .body(body)
                .retrieve()
                .body(String::class.java)
                ?: error("Gemini response is empty")

            parseAskOutput(extractGeminiOutputText(responseBody) ?: error("Gemini text is empty"), question, "GEMINI")
        }.getOrElse { providerErrors += buildFallbackReason("Gemini", it); null }
    }

    fun askWithClaude(
        question: String,
        copilot: TodayCopilotResponse,
        ideas: List<com.giwon.assistant.features.idea.dto.IdeaDetailResponse>,
        recentHistory: List<CopilotHistoryEntity>,
        goodExamples: List<CopilotHistoryEntity> = emptyList(),
        badExamples: List<CopilotHistoryEntity> = emptyList(),
        providerErrors: MutableList<String>,
    ): CopilotAskResponse? {
        if (!claudeEnabled || anthropicApiKey.isBlank()) return null

        return runCatching {
            val prompt = buildAskPrompt(question, copilot, ideas, recentHistory, goodExamples, badExamples)
            val body = mapOf(
                "model" to anthropicProperties.model,
                "max_tokens" to 512,
                "messages" to listOf(mapOf("role" to "user", "content" to prompt)),
            )
            val responseBody = claudeRestClient.post()
                .uri("/v1/messages")
                .header("x-api-key", anthropicApiKey)
                .header("anthropic-version", "2023-06-01")
                .header("Content-Type", "application/json")
                .body(body)
                .retrieve()
                .body(String::class.java)
                ?: error("Claude response is empty")

            val outputText = objectMapper.readTree(responseBody)
                .path("content").takeIf { it.isArray }
                ?.firstOrNull { it.path("type").asText() == "text" }
                ?.path("text")?.asText()?.takeIf { it.isNotBlank() }
                ?: error("Claude content is empty")

            parseAskOutput(outputText, question, "CLAUDE")
        }.getOrElse { providerErrors += buildFallbackReason("Claude", it); null }
    }

    fun askWithOpenAi(
        question: String,
        copilot: TodayCopilotResponse,
        ideas: List<com.giwon.assistant.features.idea.dto.IdeaDetailResponse>,
        recentHistory: List<CopilotHistoryEntity>,
        goodExamples: List<CopilotHistoryEntity> = emptyList(),
        badExamples: List<CopilotHistoryEntity> = emptyList(),
        providerErrors: MutableList<String>,
    ): CopilotAskResponse? {
        if (!openAiEnabled || openAiApiKey.isBlank()) return null

        return runCatching {
            val prompt = buildAskPrompt(question, copilot, ideas, recentHistory, goodExamples, badExamples)
            val responseBody = openAiRestClient.post()
                .uri("/v1/responses")
                .header("Authorization", "Bearer $openAiApiKey")
                .header("Content-Type", "application/json")
                .body(mapOf("model" to openAiProperties.model, "input" to prompt))
                .retrieve()
                .body(String::class.java)
                ?: error("OpenAI response is empty")

            parseAskOutput(extractOpenAiOutputText(responseBody) ?: error("OpenAI output_text is empty"), question, "OPENAI")
        }.getOrElse { providerErrors += buildFallbackReason("OpenAI", it); null }
    }

    fun detectIntent(normalizedQuestion: String): LocalIntent =
        when {
            containsAny(normalizedQuestion, listOf("언제", "시간", "일정", "타임", "오전", "오후")) -> LocalIntent.TIME
            containsAny(normalizedQuestion, listOf("아이디어", "기능", "기획", "만들", "개발")) -> LocalIntent.IDEA
            containsAny(normalizedQuestion, listOf("리스크", "위험", "막힐", "문제", "이슈")) -> LocalIntent.RISK
            containsAny(normalizedQuestion, listOf("요약", "정리", "한줄", "브리핑")) -> LocalIntent.SUMMARY
            else -> LocalIntent.PRIORITY
        }

    fun buildSuggestedActionPlans(
        suggestedActions: List<String>,
        intent: LocalIntent,
    ): List<CopilotSuggestedActionPlan> {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        return suggestedActions.mapIndexed { index, action ->
            val normalized = action.lowercase()
            val priority = when {
                containsAny(normalized, listOf("바로", "우선", "핵심", "결정", "정리")) -> "HIGH"
                intent == LocalIntent.RISK -> "HIGH"
                intent == LocalIntent.TIME || intent == LocalIntent.IDEA -> if (index == 0) "HIGH" else "MEDIUM"
                else -> if (index == 0) "HIGH" else if (index == 1) "MEDIUM" else "LOW"
            }
            val dueDate = when {
                containsAny(normalized, listOf("바로", "지금", "오늘")) -> nextDueTime(now, 18, 0)
                containsAny(normalized, listOf("오후", "17:30", "정리", "기록")) -> nextDueTime(now, 17, 30)
                intent == LocalIntent.PRIORITY || intent == LocalIntent.RISK ->
                    if (index == 0) nextDueTime(now, 18, 0) else nextDueTime(now.plusDays(1), 9, 0)
                intent == LocalIntent.TIME ->
                    if (index == 0) nextDueTime(now, 13, 0) else nextDueTime(now, 17, 30)
                intent == LocalIntent.IDEA -> nextDueTime(now.plusDays(index.toLong()), 14, 0)
                else -> if (index == 0) nextDueTime(now.plusDays(1), 10, 0) else nextDueTime(now.plusDays(2), 18, 0)
            }
            CopilotSuggestedActionPlan(
                title = action,
                priority = priority,
                dueDate = dueDate.toString(),
                dueLabel = buildDueLabel(dueDate, now),
                reason = buildSuggestedActionReason(intent, index, action),
            )
        }
    }

    internal fun extractOpenAiOutputText(responseBody: String): String? {
        val root = objectMapper.readTree(responseBody)
        root.path("output_text").takeIf { !it.isMissingNode && !it.isNull }
            ?.asText()?.trim()?.takeIf { it.isNotBlank() }?.let { return it }

        return root.path("output").takeIf(JsonNode::isArray)
            ?.flatMap { outputNode ->
                outputNode.path("content").takeIf(JsonNode::isArray)
                    ?.mapNotNull { it.path("text").takeIf { n -> !n.isMissingNode && !n.isNull }?.asText()?.trim()?.takeIf(String::isNotBlank) }
                    .orEmpty()
            }
            .orEmpty()
            .takeIf { it.isNotEmpty() }
            ?.joinToString("\n")
    }

    internal fun extractGeminiOutputText(responseBody: String): String? =
        objectMapper.readTree(responseBody)
            .path("candidates").takeIf(JsonNode::isArray)
            ?.flatMap { candidate ->
                candidate.path("content").path("parts").takeIf(JsonNode::isArray)
                    ?.mapNotNull { it.path("text").takeIf { n -> !n.isMissingNode && !n.isNull }?.asText()?.trim()?.takeIf(String::isNotBlank) }
                    .orEmpty()
            }
            .orEmpty()
            .takeIf { it.isNotEmpty() }
            ?.joinToString("\n")

    fun parseAskOutput(outputText: String, question: String, source: String): CopilotAskResponse {
        val lines = outputText.lines().map { it.trim() }.filter { it.isNotBlank() }
        val intent = detectIntent(question.lowercase().replace(" ", ""))
        val answer = lines.firstOrNull { it.startsWith("ANSWER:") }
            ?.substringAfter("ANSWER:")?.trim()?.ifBlank { outputText.take(160) }
            ?: outputText.take(160)
        val suggestedActions = extractBulletSection(lines, "SUGGESTED_ACTIONS:").ifEmpty {
            listOf("우선순위 작업 먼저 진행", "중요 결정 1건 확정", "끝난 뒤 다음 액션 정리")
        }
        return CopilotAskResponse(
            question = question,
            answer = answer,
            intent = intent.name,
            reasoning = extractBulletSection(lines, "REASONING:").ifEmpty {
                listOf("오늘 브리핑과 우선순위 흐름을 기준으로 답변했습니다.")
            },
            suggestedActions = suggestedActions,
            suggestedActionPlans = buildSuggestedActionPlans(suggestedActions, intent),
            source = source,
            generatedAt = OffsetDateTime.now().toString(),
        )
    }

    fun extractBulletSection(lines: List<String>, header: String): List<String> {
        val startIndex = lines.indexOfFirst { it == header }
        if (startIndex == -1) return emptyList()
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

    private fun buildAskPrompt(
        question: String,
        copilot: TodayCopilotResponse,
        ideas: List<com.giwon.assistant.features.idea.dto.IdeaDetailResponse>,
        recentHistory: List<CopilotHistoryEntity> = emptyList(),
        goodExamples: List<CopilotHistoryEntity> = emptyList(),
        badExamples: List<CopilotHistoryEntity> = emptyList(),
    ): String {
        val historySection = if (recentHistory.isNotEmpty()) {
            "\n이전 대화 (최근 ${recentHistory.size}건):\n" +
                recentHistory.joinToString("\n") { "Q: ${it.question}\nA: ${it.answer}" }
        } else ""
        val goodSection = if (goodExamples.isNotEmpty()) {
            "\n[사용자가 좋아했던 답변 예시 - 이 스타일을 참고해줘]\n" +
                goodExamples.joinToString("\n") { "Q: ${it.question}\nA: ${it.answer}" }
        } else ""
        val badSection = if (badExamples.isNotEmpty()) {
            "\n[사용자가 싫어했던 답변 예시 - 이런 스타일은 피해줘]\n" +
                badExamples.joinToString("\n") { "Q: ${it.question}\nA: ${it.answer}" }
        } else ""

        return """
        너는 개인 생산성 코파일럿이다.
        아래 오늘 컨텍스트와 이전 대화를 기준으로 질문에 짧고 실행 가능한 답을 해줘.
        응답은 반드시 아래 형식을 지켜.

        ANSWER: 한두 문장 답변
        REASONING:
        - 근거 1
        - 근거 2
        - 근거 3
        SUGGESTED_ACTIONS:
        - 액션 1
        - 액션 2
        - 액션 3

        오늘 헤드라인: ${copilot.headline}
        오늘 우선순위: ${copilot.topPriority}
        다음 액션: ${copilot.suggestedNextAction}
        리스크:
        ${copilot.risks.joinToString("\n") { "- $it" }}
        최근 아이디어:
        ${ideas.joinToString("\n") { "- ${it.title} (${it.status})" }}
        $historySection$goodSection$badSection
        사용자 질문:
        $question
        """.trimIndent()
    }

    fun buildFallbackReason(providerName: String, throwable: Throwable): String =
        when (throwable) {
            is RestClientResponseException -> when (throwable.statusCode.value()) {
                401 -> "$providerName 인증 실패(401)"
                429 -> "$providerName rate limit 또는 quota 초과(429)"
                in 500..599 -> "$providerName 서버 오류(${throwable.statusCode.value()})"
                else -> "$providerName 요청 실패(${throwable.statusCode.value()})"
            }
            else -> "$providerName 응답 처리 실패"
        }

    private fun buildSuggestedActionReason(intent: LocalIntent, index: Int, action: String): String =
        when (intent) {
            LocalIntent.PRIORITY -> if (index == 0) "오늘 최우선 작업 흐름과 직접 연결되는 액션이라 먼저 처리하는 게 좋다." else "핵심 작업 다음 순서로 이어붙이기 좋은 작업이다."
            LocalIntent.TIME -> "시간 블록 기준으로 바로 배치할 수 있는 액션이다."
            LocalIntent.IDEA -> if (index == 0) "아이디어를 실제 작업으로 전환하는 첫 단계라 빠르게 검증하는 게 좋다." else "확장 전에 작은 단위로 검증하기 좋은 액션이다."
            LocalIntent.RISK -> "리스크를 줄이기 위해 오늘 안에 정리하거나 고정해두는 편이 좋다."
            LocalIntent.SUMMARY -> if (action.contains("정리")) "마감 전 정리 성격이 강해서 뒤 블록에 두는 게 자연스럽다." else "오늘 전체 흐름을 닫는 데 필요한 액션이다."
        }

    private fun nextDueTime(base: OffsetDateTime, hour: Int, minute: Int): OffsetDateTime {
        val candidate = base.withHour(hour).withMinute(minute).withSecond(0).withNano(0)
        return if (candidate.isAfter(OffsetDateTime.now(ZoneOffset.UTC))) candidate else candidate.plusDays(1)
    }

    private fun buildDueLabel(dueDate: OffsetDateTime, now: OffsetDateTime): String {
        val dueLocalDate = dueDate.toLocalDate()
        val nowLocalDate = now.toLocalDate()
        return when {
            dueLocalDate.isEqual(nowLocalDate) -> "오늘 ${dueDate.hour.toString().padStart(2, '0')}:${dueDate.minute.toString().padStart(2, '0')}"
            dueLocalDate.isEqual(nowLocalDate.plusDays(1)) -> "내일 ${dueDate.hour.toString().padStart(2, '0')}:${dueDate.minute.toString().padStart(2, '0')}"
            else -> "${dueDate.monthValue}/${dueDate.dayOfMonth} ${dueDate.hour.toString().padStart(2, '0')}:${dueDate.minute.toString().padStart(2, '0')}"
        }
    }

    private fun containsAny(question: String, keywords: List<String>): Boolean =
        keywords.any { question.contains(it) }
}

enum class LocalIntent { PRIORITY, TIME, IDEA, RISK, SUMMARY }
