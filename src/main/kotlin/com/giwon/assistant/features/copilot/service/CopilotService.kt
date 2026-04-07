package com.giwon.assistant.features.copilot.service

import com.giwon.assistant.features.briefing.service.BriefingService
import com.giwon.assistant.features.briefing.service.CalendarProvider
import com.giwon.assistant.features.briefing.service.NewsProvider
import com.giwon.assistant.features.briefing.service.WeatherProvider
import com.giwon.assistant.features.copilot.dto.CopilotIdeaAction
import com.giwon.assistant.features.copilot.dto.CopilotTimeSuggestion
import com.giwon.assistant.features.copilot.dto.TodayCopilotResponse
import com.giwon.assistant.features.idea.service.IdeaService
import com.giwon.assistant.features.planner.service.PlannerService
import org.springframework.stereotype.Service
import java.time.OffsetDateTime

@Service
class CopilotService(
    private val briefingService: BriefingService,
    private val plannerService: PlannerService,
    private val ideaService: IdeaService,
    private val weatherProvider: WeatherProvider,
    private val calendarProvider: CalendarProvider,
    private val newsProvider: NewsProvider,
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
}
