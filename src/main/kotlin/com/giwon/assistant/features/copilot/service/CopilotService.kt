package com.giwon.assistant.features.copilot.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.giwon.assistant.features.briefing.service.BriefingService
import com.giwon.assistant.features.briefing.service.CalendarProvider
import com.giwon.assistant.features.briefing.service.NewsProvider
import com.giwon.assistant.features.briefing.service.WeatherProvider
import com.giwon.assistant.features.copilot.dto.CopilotAskResponse
import com.giwon.assistant.features.copilot.dto.CopilotHistoryResponse
import com.giwon.assistant.features.copilot.dto.TodayCopilotResponse
import com.giwon.assistant.features.copilot.dto.CopilotIdeaAction
import com.giwon.assistant.features.copilot.dto.CopilotTimeSuggestion
import com.giwon.assistant.features.copilot.entity.CopilotHistoryEntity
import com.giwon.assistant.features.copilot.repository.CopilotHistoryRepository
import com.giwon.assistant.features.idea.service.IdeaService
import com.giwon.assistant.features.idea.service.AssistantOpenAiProperties
import com.giwon.assistant.features.planner.service.PlannerService
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException
import java.time.OffsetDateTime
import org.springframework.beans.factory.annotation.Value

@Service
class CopilotService(
    private val briefingService: BriefingService,
    private val plannerService: PlannerService,
    private val ideaService: IdeaService,
    private val copilotHistoryRepository: CopilotHistoryRepository,
    private val weatherProvider: WeatherProvider,
    private val calendarProvider: CalendarProvider,
    private val newsProvider: NewsProvider,
    private val openAiRestClient: RestClient,
    private val openAiProperties: AssistantOpenAiProperties,
    private val objectMapper: ObjectMapper,
    @Value("\${assistant.integrations.openai-enabled:false}") private val openAiEnabled: Boolean,
    @Value("\${OPENAI_API_KEY:}") private val openAiApiKey: String,
) {
    fun getTodayCopilot(): TodayCopilotResponse {
        val briefing = briefingService.getTodayBriefing(weatherProvider, calendarProvider, newsProvider)
        val todayPlan = plannerService.getTodayPlan()
        val activeIdeas = ideaService.getAll()
            .filter { it.status != "DONE" }
            .take(3)

        val firstCalendarItem = briefing.calendar.firstOrNull()
        val firstHeadline = briefing.headlines.firstOrNull()
        val primaryTask = briefing.tasks.firstOrNull()?.title ?: todayPlan.topPriorities.firstOrNull() ?: "핵심 작업 정리"
        val topPriority = buildTopPriority(primaryTask, firstCalendarItem?.title)

        return TodayCopilotResponse(
            generatedAt = OffsetDateTime.now().toString(),
            headline = "오늘은 ${primaryTask}부터 끝내는 흐름이 가장 효율적이다.",
            overview = buildOverview(
                weatherCondition = briefing.weather.condition,
                temperature = briefing.weather.temperatureCelsius,
                firstEventTitle = firstCalendarItem?.title,
                firstHeadlineTitle = firstHeadline?.title,
            ),
            topPriority = topPriority,
            suggestedNextAction = buildSuggestedNextAction(primaryTask, activeIdeas.firstOrNull()?.title),
            risks = buildRisks(briefing, todayPlan),
            recommendedIdeas = activeIdeas.map { idea ->
                CopilotIdeaAction(
                    id = idea.id,
                    title = idea.title,
                    status = idea.status,
                    recommendedAction = buildIdeaAction(idea.status, idea.suggestedActions.firstOrNull()),
                )
            },
            todayFlow = buildTodayFlow(todayPlan.topPriorities, briefing.calendar.map { it.time to it.title })
        )
    }

    fun ask(question: String): CopilotAskResponse {
        val copilot = getTodayCopilot()
        val ideas = ideaService.getAll().take(3)
        val answer = runCatching {
            if (openAiEnabled && openAiApiKey.isNotBlank()) {
                askWithOpenAi(question, copilot, ideas)
            } else {
                localAsk(question, copilot, ideas)
            }
        }.getOrElse { throwable ->
            localAsk(
                question = question,
                copilot = copilot,
                ideas = ideas,
                fallbackReason = buildFallbackReason(throwable),
            )
        }

        val response = answer.copy(
            question = question,
            generatedAt = OffsetDateTime.now().toString(),
        )
        copilotHistoryRepository.save(response.toEntity())
        return response
    }

    fun getRecentHistory(): List<CopilotHistoryResponse> =
        copilotHistoryRepository.findTop10ByOrderByGeneratedAtDesc().map { it.toResponse() }

    private fun buildTopPriority(primaryTask: String, firstEventTitle: String?): String =
        if (firstEventTitle == null) {
            "오전에는 ${primaryTask}를 단독 집중 작업으로 확보하는 게 좋다."
        } else {
            "${firstEventTitle} 전에 ${primaryTask}의 핵심 결정이나 구현 1건을 먼저 끝내는 게 좋다."
        }

    private fun buildOverview(
        weatherCondition: String,
        temperature: Int,
        firstEventTitle: String?,
        firstHeadlineTitle: String?,
    ): String {
        val eventPart = firstEventTitle?.let { "첫 일정은 ${it}이고" } ?: "확정된 일정은 많지 않고"
        val headlinePart = firstHeadlineTitle?.let { "가장 먼저 볼 뉴스는 '${it}'이다." } ?: "뉴스 이슈는 크지 않다."
        return "현재 날씨는 ${weatherCondition}, ${temperature}도 수준이다. ${eventPart} 오늘은 오전 집중도를 지키는 게 중요하다. ${headlinePart}"
    }

    private fun buildSuggestedNextAction(primaryTask: String, firstIdeaTitle: String?): String =
        if (firstIdeaTitle == null) {
            "${primaryTask} 관련 작업을 90분 블록으로 먼저 확보하고, 끝난 뒤 브리핑을 한 번 더 갱신한다."
        } else {
            "${primaryTask}를 먼저 처리한 뒤 '${firstIdeaTitle}'를 다음 액션 후보로 검토하는 게 좋다."
        }

    private fun buildRisks(
        briefing: com.giwon.assistant.features.briefing.dto.TodayBriefingResponse,
        todayPlan: com.giwon.assistant.features.planner.dto.TodayPlanResponse,
    ): List<String> {
        val calendarRisk = if (briefing.calendar.size >= 2) {
            "일정이 ${briefing.calendar.size}건이라 중간에 집중 흐름이 끊길 수 있다."
        } else {
            "자유 시간이 많은 대신 우선순위가 흐려질 수 있다."
        }
        val reminderRisk = todayPlan.reminders.firstOrNull() ?: "중요 작업 종료 후 정리 시간을 따로 잡는 게 좋다."
        return listOf(
            calendarRisk,
            reminderRisk,
            "새 아이디어를 바로 구현으로 넘기기보다 기존 진행 중 항목부터 닫는 편이 효율적이다."
        )
    }

    private fun buildIdeaAction(status: String, suggestedAction: String?): String =
        when (status) {
            "OPEN" -> suggestedAction ?: "요약된 액션 중 가장 작은 작업 단위 하나를 먼저 시작"
            "IN_PROGRESS" -> "현재 진행 중인 내용을 기준으로 다음 결정 1건만 확정"
            else -> suggestedAction ?: "상태 확인 필요"
        }

    private fun buildTodayFlow(
        topPriorities: List<String>,
        calendarItems: List<Pair<String, String>>,
    ): List<CopilotTimeSuggestion> {
        val priorityOne = topPriorities.getOrNull(0) ?: "핵심 작업"
        val priorityTwo = topPriorities.getOrNull(1) ?: "연결 작업"
        val priorityThree = topPriorities.getOrNull(2) ?: "정리 작업"

        return listOf(
            CopilotTimeSuggestion(
                time = "09:00",
                focus = priorityOne,
                reason = "가장 에너지 높은 시간대에 중요한 결정이나 구현을 먼저 끝내기 좋다."
            ),
            CopilotTimeSuggestion(
                time = calendarItems.firstOrNull()?.first ?: "13:00",
                focus = calendarItems.firstOrNull()?.second ?: priorityTwo,
                reason = "오전 집중 작업 이후에는 연계 작업이나 일정 대응으로 전환하는 흐름이 자연스럽다."
            ),
            CopilotTimeSuggestion(
                time = "17:30",
                focus = priorityThree,
                reason = "마감 전에는 정리, 기록, 다음 액션 정의가 가장 효율적이다."
            )
        )
    }

    private fun askWithOpenAi(
        question: String,
        copilot: TodayCopilotResponse,
        ideas: List<com.giwon.assistant.features.idea.dto.IdeaDetailResponse>,
    ): CopilotAskResponse {
        val prompt = buildAskPrompt(question, copilot, ideas)
        val body = mapOf(
            "model" to openAiProperties.model,
            "input" to prompt,
        )

        val responseBody = openAiRestClient.post()
            .uri("/v1/responses")
            .header("Authorization", "Bearer $openAiApiKey")
            .header("Content-Type", "application/json")
            .body(body)
            .retrieve()
            .body(String::class.java)
            ?: error("OpenAI response is empty")

        val outputText = extractOpenAiOutputText(responseBody)
            ?: error("OpenAI output_text is empty")

        return parseAskOutput(outputText, question)
    }

    internal fun extractOpenAiOutputText(responseBody: String): String? {
        val root = objectMapper.readTree(responseBody)

        root.path("output_text").takeIf { !it.isMissingNode && !it.isNull }
            ?.asText()
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?.let { return it }

        val contentTexts = root.path("output")
            .takeIf(JsonNode::isArray)
            ?.flatMap { outputNode ->
                outputNode.path("content")
                    .takeIf(JsonNode::isArray)
                    ?.mapNotNull { contentNode ->
                        contentNode.path("text")
                            .takeIf { !it.isMissingNode && !it.isNull }
                            ?.asText()
                            ?.trim()
                            ?.takeIf(String::isNotBlank)
                    }
                    .orEmpty()
            }
            .orEmpty()

        return contentTexts.takeIf { it.isNotEmpty() }?.joinToString("\n")
    }

    private fun buildAskPrompt(
        question: String,
        copilot: TodayCopilotResponse,
        ideas: List<com.giwon.assistant.features.idea.dto.IdeaDetailResponse>,
    ): String =
        """
        너는 개인 생산성 코파일럿이다.
        아래 오늘 컨텍스트를 기준으로 질문에 짧고 실행 가능한 답을 해줘.
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

        사용자 질문:
        $question
        """.trimIndent()

    private fun parseAskOutput(outputText: String, question: String): CopilotAskResponse {
        val lines = outputText.lines().map { it.trim() }.filter { it.isNotBlank() }
        val answer = lines.firstOrNull { it.startsWith("ANSWER:") }
            ?.substringAfter("ANSWER:")
            ?.trim()
            ?.ifBlank { outputText.take(160) }
            ?: outputText.take(160)

        return CopilotAskResponse(
            question = question,
            answer = answer,
            reasoning = extractBulletSection(lines, "REASONING:").ifEmpty {
                listOf("오늘 브리핑과 우선순위 흐름을 기준으로 답변했습니다.")
            },
            suggestedActions = extractBulletSection(lines, "SUGGESTED_ACTIONS:").ifEmpty {
                listOf("우선순위 작업 먼저 진행", "중요 결정 1건 확정", "끝난 뒤 다음 액션 정리")
            },
            source = "OPENAI",
            generatedAt = OffsetDateTime.now().toString(),
        )
    }

    private fun localAsk(
        question: String,
        copilot: TodayCopilotResponse,
        ideas: List<com.giwon.assistant.features.idea.dto.IdeaDetailResponse>,
        fallbackReason: String? = null,
    ): CopilotAskResponse {
        val lowered = question.lowercase()
        val firstIdea = ideas.firstOrNull()
        val answer = when {
            lowered.contains("우선") || lowered.contains("먼저") -> copilot.topPriority
            lowered.contains("아이디어") -> firstIdea?.let { "'${it.title}'부터 보는 게 좋다. ${it.suggestedActions.firstOrNull() ?: "가장 작은 작업 단위 하나를 먼저 시작해."}" }
                ?: "지금은 새 아이디어보다 오늘 우선순위 작업부터 끝내는 게 좋다."
            lowered.contains("일정") || lowered.contains("시간") -> copilot.todayFlow.firstOrNull()?.let { "${it.time}에는 ${it.focus}에 집중하는 흐름이 좋다." }
                ?: copilot.suggestedNextAction
            lowered.contains("뉴스") || lowered.contains("헤드라인") -> copilot.overview
            else -> "${copilot.headline} ${copilot.suggestedNextAction}"
        }

        return CopilotAskResponse(
            question = question,
            answer = answer,
            reasoning = listOf(
                copilot.topPriority,
                copilot.risks.firstOrNull() ?: "중간 컨텍스트 전환을 줄이는 게 좋다.",
                firstIdea?.let { "최근 아이디어 '${it.title}'는 ${it.status} 상태다." } ?: "최근 아이디어보다 현재 우선순위 작업이 중요하다."
            ),
            suggestedActions = listOf(
                copilot.suggestedNextAction,
                copilot.todayFlow.firstOrNull()?.focus ?: "오전 집중 작업 확보",
                firstIdea?.suggestedActions?.firstOrNull() ?: "작업 종료 후 다음 액션 기록"
            ),
            source = "RULE_BASED",
            fallbackReason = fallbackReason,
            generatedAt = OffsetDateTime.now().toString(),
        )
    }

    private fun buildFallbackReason(throwable: Throwable): String =
        when (throwable) {
            is RestClientResponseException -> {
                val statusCode = throwable.statusCode.value()
                when (statusCode) {
                    401 -> "OpenAI 인증 실패(401)"
                    429 -> "OpenAI rate limit 또는 quota 초과(429)"
                    in 500..599 -> "OpenAI 서버 오류(${statusCode})"
                    else -> "OpenAI 요청 실패(${statusCode})"
                }
            }
            else -> "OpenAI 응답 처리 실패"
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

    private fun CopilotAskResponse.toEntity(): CopilotHistoryEntity =
        CopilotHistoryEntity(
            id = "COPILOT-${java.util.UUID.randomUUID()}",
            generatedAt = OffsetDateTime.parse(generatedAt),
            question = question,
            answer = answer,
            reasoning = objectMapper.writeValueAsString(reasoning),
            suggestedActions = objectMapper.writeValueAsString(suggestedActions),
            source = source,
        )

    private fun CopilotHistoryEntity.toResponse(): CopilotHistoryResponse =
        CopilotHistoryResponse(
            id = id,
            question = question,
            answer = answer,
            reasoning = objectMapper.readValue(reasoning, objectMapper.typeFactory.constructCollectionType(List::class.java, String::class.java)),
            suggestedActions = objectMapper.readValue(suggestedActions, objectMapper.typeFactory.constructCollectionType(List::class.java, String::class.java)),
            source = source,
            generatedAt = generatedAt.toString(),
        )
}
