package com.giwon.assistant.features.copilot.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.giwon.assistant.features.briefing.service.BriefingService
import com.giwon.assistant.features.briefing.service.CalendarProvider
import com.giwon.assistant.features.briefing.service.NewsProvider
import com.giwon.assistant.features.briefing.service.WeatherProvider
import com.giwon.assistant.features.checkin.service.DailyConditionCheckinService
import com.giwon.assistant.features.copilot.dto.CopilotAskResponse
import com.giwon.assistant.features.copilot.dto.CopilotHistoryResponse
import com.giwon.assistant.features.copilot.dto.CopilotIdeaAction
import com.giwon.assistant.features.copilot.dto.CopilotOperatingMode
import com.giwon.assistant.features.copilot.dto.CopilotTimeSuggestion
import com.giwon.assistant.features.copilot.dto.TodayCopilotResponse
import com.giwon.assistant.features.copilot.entity.CopilotHistoryEntity
import com.giwon.assistant.features.copilot.repository.CopilotHistoryRepository
import com.giwon.assistant.features.idea.service.IdeaService
import com.giwon.assistant.features.planner.dto.TodayPlanResponse
import com.giwon.assistant.features.planner.service.PlannerService
import com.giwon.assistant.features.routine.service.DailyRoutineService
import org.springframework.stereotype.Service
import java.time.OffsetDateTime

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
    private val llmService: CopilotLlmService,
    private val objectMapper: ObjectMapper,
) {
    fun getTodayCopilot(): TodayCopilotResponse {
        val briefing = briefingService.getTodayBriefing(weatherProvider, calendarProvider, newsProvider)
        val todayPlan = plannerService.getTodayPlan()
        val dailyRoutine = dailyRoutineService.getDailyRoutine(null)
        val dailyCondition = dailyConditionCheckinService.getCondition(null)
        val activeIdeas = ideaService.getAll().filter { it.status != "DONE" }.take(3)

        val firstCalendarItem = briefing.calendar.firstOrNull()
        val firstHeadline = briefing.headlines.firstOrNull()
        val primaryTask = briefing.tasks.firstOrNull()?.title ?: todayPlan.topPriorities.firstOrNull() ?: "핵심 작업 정리"
        val baseTopPriority = buildTopPriority(primaryTask, firstCalendarItem?.title)
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
            topPriority = adjustTopPriorityByCondition(baseTopPriority, dailyRoutine, dailyCondition.readinessScore),
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

    fun ask(question: String): CopilotAskResponse {
        val copilot = getTodayCopilot()
        val todayPlan = plannerService.getTodayPlan()
        val ideas = ideaService.getAll().take(3)
        val recentHistory = copilotHistoryRepository.findTop10ByOrderByGeneratedAtDesc().reversed().takeLast(3)
        val goodExamples = copilotHistoryRepository.findTop3ByRatingOrderByGeneratedAtDesc(1)
        val badExamples = copilotHistoryRepository.findTop3ByRatingOrderByGeneratedAtDesc(-1)
        val providerErrors = mutableListOf<String>()

        val answer = llmService.askWithGemini(question, copilot, ideas, recentHistory, goodExamples, badExamples, providerErrors)
            ?: llmService.askWithClaude(question, copilot, ideas, recentHistory, goodExamples, badExamples, providerErrors)
            ?: llmService.askWithOpenAi(question, copilot, ideas, recentHistory, goodExamples, badExamples, providerErrors)
            ?: localAsk(question, copilot, todayPlan, ideas, providerErrors.takeIf { it.isNotEmpty() }?.joinToString(" | "))

        val response = answer.copy(question = question, generatedAt = OffsetDateTime.now().toString())
        copilotHistoryRepository.save(response.toEntity())
        return response
    }

    fun getRecentHistory(): List<CopilotHistoryResponse> =
        copilotHistoryRepository.findTop10ByOrderByGeneratedAtDesc().map { it.toResponse() }

    fun rateHistory(id: String, rating: Int) {
        val entity = copilotHistoryRepository.findById(id).orElseThrow { NoSuchElementException("History not found: $id") }
        entity.rating = rating
        copilotHistoryRepository.save(entity)
    }

    // ── TodayCopilot 빌더 ──────────────────────────────────────────────────────

    private fun buildOperatingMode(
        dailyRoutine: com.giwon.assistant.features.routine.dto.DailyRoutineResponse,
        dailyCondition: com.giwon.assistant.features.checkin.dto.DailyConditionCheckinResponse,
    ): CopilotOperatingMode = when {
        dailyCondition.readinessScore < 45 || dailyRoutine.riskLevel == "HIGH" ->
            CopilotOperatingMode("RESET", "Reset Mode", "지금은 생산성 극대화보다 컨디션 복구와 기본 루틴 정리가 우선이야.", 20)
        dailyRoutine.recoveryScore < 45 || dailyCondition.sleepQuality <= 2 ->
            CopilotOperatingMode("RECOVERY", "Recovery Mode", "회복 점수가 낮아. 긴 작업보다 회복 블록과 가벼운 정리 작업이 맞는다.", 30)
        dailyCondition.readinessScore >= 75 && dailyRoutine.energyScore >= 70 && dailyRoutine.completedCount >= 2 ->
            CopilotOperatingMode("DEEP_FOCUS", "Deep Focus Mode", "루틴과 컨디션이 안정적이라 지금은 길고 무거운 집중 블록을 잡아도 된다.", 90)
        else ->
            CopilotOperatingMode("STEADY", "Steady Mode", "기본 흐름은 괜찮아. 45~60분 단위로 끊어서 안정적으로 진행하는 게 좋다.", 50)
    }

    private fun buildTopPriority(primaryTask: String, firstEventTitle: String?): String =
        if (firstEventTitle == null) "오전에는 ${primaryTask}를 단독 집중 작업으로 확보하는 게 좋다."
        else "${firstEventTitle} 전에 ${primaryTask}의 핵심 결정이나 구현 1건을 먼저 끝내는 게 좋다."

    private fun adjustTopPriorityByCondition(
        existingTopPriority: String,
        dailyRoutine: com.giwon.assistant.features.routine.dto.DailyRoutineResponse,
        readinessScore: Int,
    ): String = when {
        readinessScore < 45 -> "지금은 강한 몰입보다 회복 후 25분 단위 집중으로 재배치하는 게 맞다. $existingTopPriority"
        dailyRoutine.completedCount == 0 -> "메인 작업 전에 비타민, 물, 약 복용 같은 기본 루틴 1개를 먼저 닫고 ${existingTopPriority}"
        else -> existingTopPriority
    }

    private fun buildHeadline(
        primaryTask: String,
        dailyRoutine: com.giwon.assistant.features.routine.dto.DailyRoutineResponse,
        readinessScore: Int,
    ): String = when {
        readinessScore < 45 -> "오늘은 ${primaryTask}를 바로 밀기보다 컨디션 복구 후 짧은 집중 블록으로 가는 편이 안전하다."
        dailyRoutine.completedCount == 0 -> "오늘은 ${primaryTask} 전에 기본 루틴 1개부터 닫는 흐름이 더 안정적이다."
        else -> "오늘은 ${primaryTask}부터 끝내는 흐름이 가장 효율적이다."
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
    ): String = when {
        dailyCondition.readinessScore < 45 ->
            "${dailyCondition.suggestions.firstOrNull() ?: "컨디션 점수 다시 체크"}부터 처리한 뒤 짧은 작업 블록으로 넘어가는 게 좋다."
        dailyRoutine.completedCount < 2 && dailyRoutine.suggestedActions.isNotEmpty() ->
            "${dailyRoutine.suggestedActions.first()}부터 처리한 뒤 ${primaryTask} 블록으로 넘어가는 게 좋다."
        firstIdeaTitle == null ->
            "${primaryTask} 관련 작업을 90분 블록으로 먼저 확보하고, 끝난 뒤 브리핑을 한 번 더 갱신한다."
        else ->
            "${primaryTask}를 먼저 처리한 뒤 '${firstIdeaTitle}'를 다음 액션 후보로 검토하는 게 좋다."
    }

    private fun buildRisks(
        briefing: com.giwon.assistant.features.briefing.dto.TodayBriefingResponse,
        todayPlan: TodayPlanResponse,
        dailyRoutine: com.giwon.assistant.features.routine.dto.DailyRoutineResponse,
        dailyCondition: com.giwon.assistant.features.checkin.dto.DailyConditionCheckinResponse,
    ): List<String> {
        val risks = mutableListOf<String>()
        if (briefing.calendar.size >= 3) risks += "오늘 일정이 ${briefing.calendar.size}개라 집중 블록 확보가 어려울 수 있다."
        if (todayPlan.topPriorities.size > 2) risks += "우선순위가 ${todayPlan.topPriorities.size}개라 실제 완료율이 낮을 수 있다."
        if (dailyRoutine.riskLevel == "HIGH") risks += "루틴 리스크 수준이 높아 오늘 집중력 유지가 어려울 수 있다."
        if (dailyCondition.readinessScore < 50) risks += "컨디션 점수(${dailyCondition.readinessScore})가 낮아 장시간 집중이 힘들 수 있다."
        if (risks.isEmpty()) risks += "현재 큰 리스크는 없어 보인다. 계획대로 진행하면 된다."
        return risks
    }

    private fun buildIdeaAction(status: String, suggestedAction: String?): String =
        when (status) {
            "OPEN" -> suggestedAction ?: "아이디어를 실행 가능한 첫 단계로 쪼개서 IN_PROGRESS로 전환하기"
            "IN_PROGRESS" -> suggestedAction ?: "진행 중인 아이디어의 다음 마일스톤 1개 완료하기"
            else -> "아이디어 상태를 점검하고 다음 액션 확정하기"
        }

    private fun buildTodayFlow(
        topPriorities: List<String>,
        calendarItems: List<Pair<String, String>>,
    ): List<CopilotTimeSuggestion> {
        val priorityOne = topPriorities.firstOrNull() ?: "핵심 작업"
        val priorityTwo = topPriorities.getOrNull(1) ?: "연결 작업"
        val priorityThree = topPriorities.getOrNull(2) ?: "정리 및 기록"
        val firstEvent = calendarItems.firstOrNull()

        return listOf(
            CopilotTimeSuggestion("09:00", priorityOne, "오전 집중력이 가장 높을 때 핵심 작업을 먼저 닫아야 하루 효율이 올라간다."),
            CopilotTimeSuggestion(
                firstEvent?.first ?: "13:00",
                firstEvent?.second ?: priorityTwo,
                if (firstEvent != null) "일정 전후로 컨텍스트 전환 비용이 생기므로 그 사이 짧은 집중 블록을 유지하는 게 좋다."
                else "오전 블록 마무리 후 오후로 자연스럽게 이어지는 흐름이다."
            ),
            CopilotTimeSuggestion("17:30", priorityThree, "마감 전에는 정리, 기록, 다음 액션 정의가 가장 효율적이다.")
        )
    }

    // ── Rule-based fallback ────────────────────────────────────────────────────

    private fun localAsk(
        question: String,
        copilot: TodayCopilotResponse,
        todayPlan: TodayPlanResponse,
        ideas: List<com.giwon.assistant.features.idea.dto.IdeaDetailResponse>,
        fallbackReason: String? = null,
    ): CopilotAskResponse {
        val intent = llmService.detectIntent(question.lowercase().replace(" ", ""))
        val firstFlow = copilot.todayFlow.firstOrNull()
        val secondFlow = copilot.todayFlow.getOrNull(1)
        val firstIdea = ideas.firstOrNull()
        val openIdeas = ideas.filter { it.status == "OPEN" || it.status == "IN_PROGRESS" }
        val topPriority = todayPlan.topPriorities.firstOrNull() ?: firstFlow?.focus ?: "핵심 작업"
        val firstTimeBlock = todayPlan.timeBlocks.firstOrNull()

        val answer = when (intent) {
            LocalIntent.PRIORITY -> "${topPriority}부터 끝내는 게 좋다. ${firstTimeBlock?.start ?: "지금"}부터 60~90분 집중 블록으로 먼저 고정해."
            LocalIntent.TIME -> "${firstFlow?.time ?: "09:00"}에는 ${firstFlow?.focus ?: topPriority}에 집중하고, ${secondFlow?.time ?: "13:00"}에는 ${secondFlow?.focus ?: "연결 작업"}으로 넘어가는 흐름이 좋다."
            LocalIntent.IDEA -> firstIdea?.let { "'${it.title}'는 ${it.status} 상태라 오늘은 ${it.suggestedActions.firstOrNull() ?: "가장 작은 실행 단위 하나"}부터 처리하는 게 좋다." }
                ?: "아이디어는 새로 확장하기보다 오늘 우선순위 작업을 먼저 닫는 게 좋다."
            LocalIntent.RISK -> "${copilot.risks.firstOrNull() ?: "중간 집중 흐름이 끊길 리스크"}가 가장 크다. 일정 사이 전환 비용을 줄이도록 작업 개수를 1~2개로 제한하는 게 좋다."
            LocalIntent.SUMMARY -> "${copilot.headline} 현재 열린 아이디어 ${openIdeas.size}건 기준으로 오늘은 핵심 1건 완료가 최우선이다."
        }

        val reasoning = when (intent) {
            LocalIntent.PRIORITY -> listOf(copilot.topPriority, "오늘 최상위 우선순위는 '${topPriority}'로 정렬돼 있다.", "열린 아이디어 ${openIdeas.size}건이라 시작점을 좁혀야 완료율이 올라간다.")
            LocalIntent.TIME -> listOf(firstFlow?.let { "${it.time} 블록은 '${it.focus}'에 맞춰져 있다." } ?: "오전 블록을 핵심 작업에 배치하는 게 유리하다.", secondFlow?.let { "${it.time}에는 '${it.focus}'로 전환하는 흐름이다." } ?: "오후 블록은 연계 작업으로 넘기는 편이 안정적이다.", todayPlan.reminders.firstOrNull() ?: "마감 전에 정리 시간을 남겨두는 게 좋다.")
            LocalIntent.IDEA -> listOf(firstIdea?.let { "최근 아이디어 '${it.title}'는 ${it.status} 상태다." } ?: "현재 진행 중 아이디어가 적다.", firstIdea?.suggestedActions?.firstOrNull() ?: "작은 실행 단위부터 시작할 때 리스크가 낮다.", "아이디어 확장보다 오늘 우선순위 완료가 전체 효율을 높인다.")
            LocalIntent.RISK -> listOf(copilot.risks.firstOrNull() ?: "컨텍스트 전환이 가장 큰 리스크다.", copilot.risks.getOrNull(1) ?: "일정 간격이 짧으면 집중이 깨질 수 있다.", "작업 동시 진행 수를 줄이면 품질과 완료율이 같이 올라간다.")
            LocalIntent.SUMMARY -> listOf(copilot.topPriority, copilot.suggestedNextAction, "열린 아이디어 ${openIdeas.size}건 기준으로 실행 순서 고정이 필요하다.")
        }

        val suggestedActions = when (intent) {
            LocalIntent.PRIORITY -> listOf("${topPriority} 60~90분 집중 블록 바로 시작", "첫 블록 종료 시 결과물 1개(결정/구현) 확정", "남은 작업은 오후 블록으로 재정렬")
            LocalIntent.TIME -> listOf("${firstFlow?.time ?: "09:00"} 블록: ${firstFlow?.focus ?: topPriority}", "${secondFlow?.time ?: "13:00"} 블록: ${secondFlow?.focus ?: "연결 작업"}", "17:30 이전에 다음 액션 3개만 기록")
            LocalIntent.IDEA -> listOf(firstIdea?.suggestedActions?.firstOrNull() ?: "아이디어를 실행 단위로 쪼개기", "오늘은 아이디어 1건만 선택해 1차 결과 만들기", "완료 후 상태를 OPEN -> IN_PROGRESS로 갱신")
            LocalIntent.RISK -> listOf("동시에 진행하는 작업을 최대 2개로 제한", "일정 사이 10분 버퍼 확보", "마감 전 30분은 정리/기록 시간으로 고정")
            LocalIntent.SUMMARY -> listOf(copilot.suggestedNextAction, firstFlow?.focus ?: "오전 핵심 작업 1건 완료", firstIdea?.suggestedActions?.firstOrNull() ?: "작업 종료 후 다음 액션 기록")
        }

        return CopilotAskResponse(
            question = question,
            answer = answer,
            intent = intent.name,
            reasoning = reasoning,
            suggestedActions = suggestedActions,
            suggestedActionPlans = llmService.buildSuggestedActionPlans(suggestedActions, intent),
            source = "RULE_BASED",
            fallbackReason = fallbackReason,
            generatedAt = OffsetDateTime.now().toString(),
        )
    }

    // ── 매핑 헬퍼 ─────────────────────────────────────────────────────────────

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
            intent = llmService.detectIntent(question.lowercase().replace(" ", "")).name,
            reasoning = objectMapper.readValue(reasoning, objectMapper.typeFactory.constructCollectionType(List::class.java, String::class.java)),
            suggestedActions = objectMapper.readValue(suggestedActions, objectMapper.typeFactory.constructCollectionType(List::class.java, String::class.java)),
            source = source,
            generatedAt = generatedAt.toString(),
            rating = rating,
        )
}
