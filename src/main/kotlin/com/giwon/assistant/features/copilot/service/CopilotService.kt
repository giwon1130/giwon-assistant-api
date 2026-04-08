package com.giwon.assistant.features.copilot.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.giwon.assistant.features.briefing.service.BriefingService
import com.giwon.assistant.features.briefing.service.CalendarProvider
import com.giwon.assistant.features.briefing.service.NewsProvider
import com.giwon.assistant.features.briefing.service.WeatherProvider
import com.giwon.assistant.features.checkin.service.DailyConditionCheckinService
import com.giwon.assistant.features.copilot.dto.CopilotAskResponse
import com.giwon.assistant.features.copilot.dto.CopilotHistoryResponse
import com.giwon.assistant.features.copilot.dto.CopilotOperatingMode
import com.giwon.assistant.features.copilot.dto.CopilotSuggestedActionPlan
import com.giwon.assistant.features.copilot.dto.TodayCopilotResponse
import com.giwon.assistant.features.copilot.dto.CopilotIdeaAction
import com.giwon.assistant.features.copilot.dto.CopilotTimeSuggestion
import com.giwon.assistant.features.copilot.entity.CopilotHistoryEntity
import com.giwon.assistant.features.copilot.repository.CopilotHistoryRepository
import com.giwon.assistant.features.idea.service.AssistantGeminiProperties
import com.giwon.assistant.features.idea.service.IdeaService
import com.giwon.assistant.features.idea.service.AssistantOpenAiProperties
import com.giwon.assistant.features.planner.dto.TodayPlanResponse
import com.giwon.assistant.features.planner.service.PlannerService
import com.giwon.assistant.features.routine.service.DailyRoutineService
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException
import java.time.OffsetDateTime
import java.time.ZoneOffset
import org.springframework.beans.factory.annotation.Value

@Service
class CopilotService(
    private val briefingService: BriefingService,
    private val plannerService: PlannerService,
    private val ideaService: IdeaService,
    private val dailyRoutineService: DailyRoutineService,
    private val dailyConditionCheckinService: DailyConditionCheckinService,
    private val copilotHistoryRepository: CopilotHistoryRepository,
    private val weatherProvider: WeatherProvider,
    private val calendarProvider: CalendarProvider,
    private val newsProvider: NewsProvider,
    private val geminiRestClient: RestClient,
    private val openAiRestClient: RestClient,
    private val geminiProperties: AssistantGeminiProperties,
    private val openAiProperties: AssistantOpenAiProperties,
    private val objectMapper: ObjectMapper,
    @Value("\${assistant.integrations.gemini-enabled:false}") private val geminiEnabled: Boolean,
    @Value("\${assistant.integrations.openai-enabled:false}") private val openAiEnabled: Boolean,
    @Value("\${GEMINI_API_KEY:}") private val geminiApiKey: String,
    @Value("\${OPENAI_API_KEY:}") private val openAiApiKey: String,
) {
    fun getTodayCopilot(): TodayCopilotResponse {
        val briefing = briefingService.getTodayBriefing(weatherProvider, calendarProvider, newsProvider)
        val todayPlan = plannerService.getTodayPlan()
        val dailyRoutine = dailyRoutineService.getDailyRoutine(null)
        val dailyCondition = dailyConditionCheckinService.getCondition(null)
        val activeIdeas = ideaService.getAll()
            .filter { it.status != "DONE" }
            .take(3)

        val firstCalendarItem = briefing.calendar.firstOrNull()
        val firstHeadline = briefing.headlines.firstOrNull()
        val primaryTask = briefing.tasks.firstOrNull()?.title ?: todayPlan.topPriorities.firstOrNull() ?: "핵심 작업 정리"
        val topPriority = buildTopPriority(primaryTask, firstCalendarItem?.title)
        val operatingMode = buildOperatingMode(dailyRoutine, dailyCondition)

        return TodayCopilotResponse(
            generatedAt = OffsetDateTime.now().toString(),
            operatingMode = operatingMode,
            headline = buildHeadline(primaryTask, dailyRoutine, dailyCondition.readinessScore),
            overview = buildOverview(
                weatherCondition = briefing.weather.condition,
                temperature = briefing.weather.temperatureCelsius,
                firstEventTitle = firstCalendarItem?.title,
                firstHeadlineTitle = firstHeadline?.title,
                dailyRoutineSummary = dailyRoutine.insight,
                conditionSummary = dailyCondition.summary,
            ),
            topPriority = buildTopPriority(topPriority, dailyRoutine, dailyCondition.readinessScore),
            suggestedNextAction = buildSuggestedNextAction(primaryTask, activeIdeas.firstOrNull()?.title, dailyRoutine, dailyCondition),
            routineSummary = dailyRoutine.insight,
            routineSuggestedAction = dailyRoutine.suggestedActions.firstOrNull()
                ?: "오늘 남은 루틴 1개를 먼저 닫고 메인 작업으로 넘어가기",
            conditionSummary = dailyCondition.summary,
            conditionSuggestedAction = dailyCondition.suggestions.firstOrNull()
                ?: "컨디션 점수를 먼저 체크하고 오늘 작업 강도 조절하기",
            conditionReadinessScore = dailyCondition.readinessScore,
            risks = buildRisks(briefing, todayPlan, dailyRoutine, dailyCondition),
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

    private fun buildOperatingMode(
        dailyRoutine: com.giwon.assistant.features.routine.dto.DailyRoutineResponse,
        dailyCondition: com.giwon.assistant.features.checkin.dto.DailyConditionCheckinResponse,
    ): CopilotOperatingMode =
        when {
            dailyCondition.readinessScore < 45 || dailyRoutine.riskLevel == "HIGH" ->
                CopilotOperatingMode(
                    code = "RESET",
                    title = "Reset Mode",
                    summary = "지금은 생산성 극대화보다 컨디션 복구와 기본 루틴 정리가 우선이야.",
                    recommendedBlockMinutes = 20,
                )

            dailyRoutine.recoveryScore < 45 || dailyCondition.sleepQuality <= 2 ->
                CopilotOperatingMode(
                    code = "RECOVERY",
                    title = "Recovery Mode",
                    summary = "회복 점수가 낮아. 긴 작업보다 회복 블록과 가벼운 정리 작업이 맞는다.",
                    recommendedBlockMinutes = 30,
                )

            dailyCondition.readinessScore >= 75 && dailyRoutine.energyScore >= 70 && dailyRoutine.completedCount >= 2 ->
                CopilotOperatingMode(
                    code = "DEEP_FOCUS",
                    title = "Deep Focus Mode",
                    summary = "루틴과 컨디션이 안정적이라 지금은 길고 무거운 집중 블록을 잡아도 된다.",
                    recommendedBlockMinutes = 90,
                )

            else ->
                CopilotOperatingMode(
                    code = "STEADY",
                    title = "Steady Mode",
                    summary = "기본 흐름은 괜찮아. 45~60분 단위로 끊어서 안정적으로 진행하는 게 좋다.",
                    recommendedBlockMinutes = 50,
                )
        }

    fun ask(question: String): CopilotAskResponse {
        val copilot = getTodayCopilot()
        val todayPlan = plannerService.getTodayPlan()
        val ideas = ideaService.getAll().take(3)
        val providerErrors = mutableListOf<String>()

        val answer = askWithGemini(question, copilot, ideas, providerErrors)
            ?: askWithOpenAi(question, copilot, ideas, providerErrors)
            ?: localAsk(
                question = question,
                copilot = copilot,
                todayPlan = todayPlan,
                ideas = ideas,
                fallbackReason = providerErrors.takeIf { it.isNotEmpty() }?.joinToString(" | "),
            )

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

    private fun buildHeadline(
        primaryTask: String,
        dailyRoutine: com.giwon.assistant.features.routine.dto.DailyRoutineResponse,
        readinessScore: Int,
    ): String =
        if (readinessScore < 45) {
            "오늘은 ${primaryTask}를 바로 밀기보다 컨디션 복구 후 짧은 집중 블록으로 가는 편이 안전하다."
        } else if (dailyRoutine.completedCount == 0) {
            "오늘은 ${primaryTask} 전에 기본 루틴 1개부터 닫는 흐름이 더 안정적이다."
        } else {
            "오늘은 ${primaryTask}부터 끝내는 흐름이 가장 효율적이다."
        }

    private fun buildOverview(
        weatherCondition: String,
        temperature: Int,
        firstEventTitle: String?,
        firstHeadlineTitle: String?,
        dailyRoutineSummary: String,
        conditionSummary: String,
    ): String {
        val eventPart = firstEventTitle?.let { "첫 일정은 ${it}이고" } ?: "확정된 일정은 많지 않고"
        val headlinePart = firstHeadlineTitle?.let { "가장 먼저 볼 뉴스는 '${it}'이다." } ?: "뉴스 이슈는 크지 않다."
        return "현재 날씨는 ${weatherCondition}, ${temperature}도 수준이다. ${eventPart} 오늘은 오전 집중도를 지키는 게 중요하다. ${headlinePart} 루틴 기준으로는 ${dailyRoutineSummary} 컨디션 기준으로는 ${conditionSummary}"
    }

    private fun buildSuggestedNextAction(
        primaryTask: String,
        firstIdeaTitle: String?,
        dailyRoutine: com.giwon.assistant.features.routine.dto.DailyRoutineResponse,
        dailyCondition: com.giwon.assistant.features.checkin.dto.DailyConditionCheckinResponse,
    ): String =
        if (dailyCondition.readinessScore < 45) {
            "${dailyCondition.suggestions.firstOrNull() ?: "컨디션 점수 다시 체크"}부터 처리한 뒤 짧은 작업 블록으로 넘어가는 게 좋다."
        } else if (dailyRoutine.completedCount < 2 && dailyRoutine.suggestedActions.isNotEmpty()) {
            "${dailyRoutine.suggestedActions.first()}부터 처리한 뒤 ${primaryTask} 블록으로 넘어가는 게 좋다."
        } else if (firstIdeaTitle == null) {
            "${primaryTask} 관련 작업을 90분 블록으로 먼저 확보하고, 끝난 뒤 브리핑을 한 번 더 갱신한다."
        } else {
            "${primaryTask}를 먼저 처리한 뒤 '${firstIdeaTitle}'를 다음 액션 후보로 검토하는 게 좋다."
        }

    private fun buildTopPriority(
        existingTopPriority: String,
        dailyRoutine: com.giwon.assistant.features.routine.dto.DailyRoutineResponse,
        readinessScore: Int,
    ): String =
        if (readinessScore < 45) {
            "지금은 강한 몰입보다 회복 후 25분 단위 집중으로 재배치하는 게 맞다. $existingTopPriority"
        } else if (dailyRoutine.completedCount == 0) {
            "메인 작업 전에 비타민, 물, 약 복용 같은 기본 루틴 1개를 먼저 닫고 ${existingTopPriority}"
        } else {
            existingTopPriority
        }

    private fun buildRisks(
        briefing: com.giwon.assistant.features.briefing.dto.TodayBriefingResponse,
        todayPlan: com.giwon.assistant.features.planner.dto.TodayPlanResponse,
        dailyRoutine: com.giwon.assistant.features.routine.dto.DailyRoutineResponse,
        dailyCondition: com.giwon.assistant.features.checkin.dto.DailyConditionCheckinResponse,
    ): List<String> {
        val calendarRisk = if (briefing.calendar.size >= 2) {
            "일정이 ${briefing.calendar.size}건이라 중간에 집중 흐름이 끊길 수 있다."
        } else {
            "자유 시간이 많은 대신 우선순위가 흐려질 수 있다."
        }
        val reminderRisk = todayPlan.reminders.firstOrNull() ?: "중요 작업 종료 후 정리 시간을 따로 잡는 게 좋다."
        val routineRisk = if (dailyRoutine.completedCount == 0) {
            "기본 루틴 체크가 아직 없어 컨디션 관리가 뒤로 밀릴 수 있다."
        } else if (dailyRoutine.weeklyCompletionRate < 40) {
            "최근 7일 루틴 완료율이 낮아 생활 리듬이 흔들릴 수 있다."
        } else {
            "루틴은 유지되고 있지만 저녁 회복 루틴이 비면 마감 집중도가 떨어질 수 있다."
        }
        val conditionRisk = when {
            dailyCondition.readinessScore < 45 -> "컨디션 준비도가 낮아서 긴 집중 블록을 바로 잡으면 효율이 급격히 떨어질 수 있다."
            dailyCondition.stress >= 4 -> "스트레스 점수가 높아서 계획보다 처리량을 줄여야 할 수 있다."
            else -> "컨디션은 안정적이지만 오후 이후 피로 누적을 따로 관리해야 한다."
        }
        return listOf(
            calendarRisk,
            reminderRisk,
            routineRisk,
            conditionRisk,
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

    private fun askWithGemini(
        question: String,
        copilot: TodayCopilotResponse,
        ideas: List<com.giwon.assistant.features.idea.dto.IdeaDetailResponse>,
        providerErrors: MutableList<String>,
    ): CopilotAskResponse? {
        if (!geminiEnabled || geminiApiKey.isBlank()) {
            return null
        }

        return runCatching {
            val prompt = buildAskPrompt(question, copilot, ideas)
            val body = mapOf(
                "contents" to listOf(
                    mapOf(
                        "parts" to listOf(
                            mapOf("text" to prompt)
                        )
                    )
                )
            )

            val responseBody = geminiRestClient.post()
                .uri { builder ->
                    builder.path("/v1beta/models/{model}:generateContent")
                        .queryParam("key", geminiApiKey)
                        .build(geminiProperties.model)
                }
                .header("Content-Type", "application/json")
                .body(body)
                .retrieve()
                .body(String::class.java)
                ?: error("Gemini response is empty")

            val outputText = extractGeminiOutputText(responseBody)
                ?: error("Gemini text is empty")

            parseAskOutput(outputText, question, "GEMINI")
        }.getOrElse { throwable ->
            providerErrors += buildFallbackReason("Gemini", throwable)
            null
        }
    }

    private fun askWithOpenAi(
        question: String,
        copilot: TodayCopilotResponse,
        ideas: List<com.giwon.assistant.features.idea.dto.IdeaDetailResponse>,
        providerErrors: MutableList<String>,
    ): CopilotAskResponse? {
        if (!openAiEnabled || openAiApiKey.isBlank()) {
            return null
        }

        return runCatching {
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

            parseAskOutput(outputText, question, "OPENAI")
        }.getOrElse { throwable ->
            providerErrors += buildFallbackReason("OpenAI", throwable)
            null
        }
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

    internal fun extractGeminiOutputText(responseBody: String): String? {
        val root = objectMapper.readTree(responseBody)

        val candidateText = root.path("candidates")
            .takeIf(JsonNode::isArray)
            ?.flatMap { candidate ->
                candidate.path("content")
                    .path("parts")
                    .takeIf(JsonNode::isArray)
                    ?.mapNotNull { part ->
                        part.path("text")
                            .takeIf { !it.isMissingNode && !it.isNull }
                            ?.asText()
                            ?.trim()
                            ?.takeIf(String::isNotBlank)
                    }
                    .orEmpty()
            }
            .orEmpty()

        return candidateText.takeIf { it.isNotEmpty() }?.joinToString("\n")
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

    private fun parseAskOutput(outputText: String, question: String, source: String): CopilotAskResponse {
        val lines = outputText.lines().map { it.trim() }.filter { it.isNotBlank() }
        val intent = detectIntent(question.lowercase().replace(" ", ""))
        val answer = lines.firstOrNull { it.startsWith("ANSWER:") }
            ?.substringAfter("ANSWER:")
            ?.trim()
            ?.ifBlank { outputText.take(160) }
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

    private fun localAsk(
        question: String,
        copilot: TodayCopilotResponse,
        todayPlan: TodayPlanResponse,
        ideas: List<com.giwon.assistant.features.idea.dto.IdeaDetailResponse>,
        fallbackReason: String? = null,
    ): CopilotAskResponse {
        val normalized = question.lowercase().replace(" ", "")
        val intent = detectIntent(normalized)

        val firstFlow = copilot.todayFlow.firstOrNull()
        val secondFlow = copilot.todayFlow.getOrNull(1)
        val firstIdea = ideas.firstOrNull()
        val openIdeas = ideas.filter { it.status == "OPEN" || it.status == "IN_PROGRESS" }
        val topPriority = todayPlan.topPriorities.firstOrNull() ?: firstFlow?.focus ?: "핵심 작업"
        val firstTimeBlock = todayPlan.timeBlocks.firstOrNull()
        val answer = when (intent) {
            LocalIntent.PRIORITY ->
                "${topPriority}부터 끝내는 게 좋다. ${firstTimeBlock?.start ?: "지금"}부터 60~90분 집중 블록으로 먼저 고정해."
            LocalIntent.TIME ->
                "${firstFlow?.time ?: "09:00"}에는 ${firstFlow?.focus ?: topPriority}에 집중하고, ${secondFlow?.time ?: "13:00"}에는 ${secondFlow?.focus ?: "연결 작업"}으로 넘어가는 흐름이 좋다."
            LocalIntent.IDEA ->
                firstIdea?.let {
                    "'${it.title}'는 ${it.status} 상태라 오늘은 ${it.suggestedActions.firstOrNull() ?: "가장 작은 실행 단위 하나"}부터 처리하는 게 좋다."
                } ?: "아이디어는 새로 확장하기보다 오늘 우선순위 작업을 먼저 닫는 게 좋다."
            LocalIntent.RISK ->
                "${copilot.risks.firstOrNull() ?: "중간 집중 흐름이 끊길 리스크"}가 가장 크다. 일정 사이 전환 비용을 줄이도록 작업 개수를 1~2개로 제한하는 게 좋다."
            LocalIntent.SUMMARY ->
                "${copilot.headline} 현재 열린 아이디어 ${openIdeas.size}건 기준으로 오늘은 핵심 1건 완료가 최우선이다."
        }

        val reasoning = when (intent) {
            LocalIntent.PRIORITY -> listOf(
                copilot.topPriority,
                "오늘 최상위 우선순위는 '${topPriority}'로 정렬돼 있다.",
                "열린 아이디어 ${openIdeas.size}건이라 시작점을 좁혀야 완료율이 올라간다.",
            )
            LocalIntent.TIME -> listOf(
                firstFlow?.let { "${it.time} 블록은 '${it.focus}'에 맞춰져 있다." } ?: "오전 블록을 핵심 작업에 배치하는 게 유리하다.",
                secondFlow?.let { "${it.time}에는 '${it.focus}'로 전환하는 흐름이다." } ?: "오후 블록은 연계 작업으로 넘기는 편이 안정적이다.",
                todayPlan.reminders.firstOrNull() ?: "마감 전에 정리 시간을 남겨두는 게 좋다.",
            )
            LocalIntent.IDEA -> listOf(
                firstIdea?.let { "최근 아이디어 '${it.title}'는 ${it.status} 상태다." } ?: "현재 진행 중 아이디어가 적다.",
                firstIdea?.suggestedActions?.firstOrNull() ?: "작은 실행 단위부터 시작할 때 리스크가 낮다.",
                "아이디어 확장보다 오늘 우선순위 완료가 전체 효율을 높인다.",
            )
            LocalIntent.RISK -> listOf(
                copilot.risks.firstOrNull() ?: "컨텍스트 전환이 가장 큰 리스크다.",
                copilot.risks.getOrNull(1) ?: "일정 간격이 짧으면 집중이 깨질 수 있다.",
                "작업 동시 진행 수를 줄이면 품질과 완료율이 같이 올라간다.",
            )
            LocalIntent.SUMMARY -> listOf(
                copilot.topPriority,
                copilot.suggestedNextAction,
                "열린 아이디어 ${openIdeas.size}건 기준으로 실행 순서 고정이 필요하다.",
            )
        }

        val suggestedActions = when (intent) {
            LocalIntent.PRIORITY -> listOf(
                "${topPriority} 60~90분 집중 블록 바로 시작",
                "첫 블록 종료 시 결과물 1개(결정/구현) 확정",
                "남은 작업은 오후 블록으로 재정렬",
            )
            LocalIntent.TIME -> listOf(
                "${firstFlow?.time ?: "09:00"} 블록: ${firstFlow?.focus ?: topPriority}",
                "${secondFlow?.time ?: "13:00"} 블록: ${secondFlow?.focus ?: "연결 작업"}",
                "17:30 이전에 다음 액션 3개만 기록",
            )
            LocalIntent.IDEA -> listOf(
                firstIdea?.suggestedActions?.firstOrNull() ?: "아이디어를 실행 단위로 쪼개기",
                "오늘은 아이디어 1건만 선택해 1차 결과 만들기",
                "완료 후 상태를 OPEN -> IN_PROGRESS로 갱신",
            )
            LocalIntent.RISK -> listOf(
                "동시에 진행하는 작업을 최대 2개로 제한",
                "일정 사이 10분 버퍼 확보",
                "마감 전 30분은 정리/기록 시간으로 고정",
            )
            LocalIntent.SUMMARY -> listOf(
                copilot.suggestedNextAction,
                firstFlow?.focus ?: "오전 핵심 작업 1건 완료",
                firstIdea?.suggestedActions?.firstOrNull() ?: "작업 종료 후 다음 액션 기록",
            )
        }

        return CopilotAskResponse(
            question = question,
            answer = answer,
            intent = intent.name,
            reasoning = reasoning,
            suggestedActions = suggestedActions,
            suggestedActionPlans = buildSuggestedActionPlans(suggestedActions, intent),
            source = "RULE_BASED",
            fallbackReason = fallbackReason,
            generatedAt = OffsetDateTime.now().toString(),
        )
    }

    private fun buildSuggestedActionPlans(
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
                    if (index == 0) nextDueTime(now, 18, 0)
                    else nextDueTime(now.plusDays(1), 9, 0)
                intent == LocalIntent.TIME ->
                    if (index == 0) nextDueTime(now, 13, 0)
                    else nextDueTime(now, 17, 30)
                intent == LocalIntent.IDEA ->
                    nextDueTime(now.plusDays(index.toLong()), 14, 0)
                else ->
                    if (index == 0) nextDueTime(now.plusDays(1), 10, 0)
                    else nextDueTime(now.plusDays(2), 18, 0)
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

    private fun buildSuggestedActionReason(intent: LocalIntent, index: Int, action: String): String =
        when (intent) {
            LocalIntent.PRIORITY -> if (index == 0) "오늘 최우선 작업 흐름과 직접 연결되는 액션이라 먼저 처리하는 게 좋다." else "핵심 작업 다음 순서로 이어붙이기 좋은 작업이다."
            LocalIntent.TIME -> "시간 블록 기준으로 바로 배치할 수 있는 액션이다."
            LocalIntent.IDEA -> if (index == 0) "아이디어를 실제 작업으로 전환하는 첫 단계라 빠르게 검증하는 게 좋다." else "확장 전에 작은 단위로 검증하기 좋은 액션이다."
            LocalIntent.RISK -> "리스크를 줄이기 위해 오늘 안에 정리하거나 고정해두는 편이 좋다."
            LocalIntent.SUMMARY -> if (action.contains("정리")) "마감 전 정리 성격이 강해서 뒤 블록에 두는 게 자연스럽다." else "오늘 전체 흐름을 닫는 데 필요한 액션이다."
        }

    private fun detectIntent(normalizedQuestion: String): LocalIntent =
        when {
            containsAny(normalizedQuestion, listOf("언제", "시간", "일정", "타임", "오전", "오후")) -> LocalIntent.TIME
            containsAny(normalizedQuestion, listOf("아이디어", "기능", "기획", "만들", "개발")) -> LocalIntent.IDEA
            containsAny(normalizedQuestion, listOf("리스크", "위험", "막힐", "문제", "이슈")) -> LocalIntent.RISK
            containsAny(normalizedQuestion, listOf("요약", "정리", "한줄", "브리핑")) -> LocalIntent.SUMMARY
            else -> LocalIntent.PRIORITY
        }

    private fun containsAny(question: String, keywords: List<String>): Boolean =
        keywords.any { question.contains(it) }

    private fun buildFallbackReason(providerName: String, throwable: Throwable): String =
        when (throwable) {
            is RestClientResponseException -> {
                val statusCode = throwable.statusCode.value()
                when (statusCode) {
                    401 -> "${providerName} 인증 실패(401)"
                    429 -> "${providerName} rate limit 또는 quota 초과(429)"
                    in 500..599 -> "${providerName} 서버 오류(${statusCode})"
                    else -> "${providerName} 요청 실패(${statusCode})"
                }
            }
            else -> "${providerName} 응답 처리 실패"
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
            intent = detectIntent(question.lowercase().replace(" ", "")).name,
            reasoning = objectMapper.readValue(reasoning, objectMapper.typeFactory.constructCollectionType(List::class.java, String::class.java)),
            suggestedActions = objectMapper.readValue(suggestedActions, objectMapper.typeFactory.constructCollectionType(List::class.java, String::class.java)),
            source = source,
            generatedAt = generatedAt.toString(),
        )

    private enum class LocalIntent {
        PRIORITY,
        TIME,
        IDEA,
        RISK,
        SUMMARY,
    }
}
