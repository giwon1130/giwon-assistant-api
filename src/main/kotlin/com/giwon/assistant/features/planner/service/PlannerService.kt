package com.giwon.assistant.features.planner.service

import com.giwon.assistant.features.planner.dto.TimeBlock
import com.giwon.assistant.features.planner.dto.TodayPlanResponse
import com.giwon.assistant.features.briefing.service.CalendarProvider
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class PlannerService(
    private val calendarProvider: CalendarProvider,
) {
    fun getTodayPlan(): TodayPlanResponse {
        val events = calendarProvider.getEvents(LocalDate.now())
        val timeBlocks = events.map { event ->
            TimeBlock(
                start = event.time,
                end = inferEndTime(event.time),
                activity = event.title,
            )
        }

        return TodayPlanResponse(
            date = LocalDate.now().toString(),
            topPriorities = listOf(
                "AI 비서 API MVP 확정",
                "허브 서비스 연동 정리",
                "아이디어 노트 구조화",
            ),
            timeBlocks = timeBlocks,
            reminders = listOf(
                "오후 작업 전에 브리핑 갱신",
                "새 아이디어는 요약 API로 먼저 정리",
            ),
        )
    }

    private fun inferEndTime(start: String): String {
        val hour = start.substringBefore(":").toIntOrNull() ?: return start
        val minute = start.substringAfter(":", "00")
        return "%02d:%s".format((hour + 1).coerceAtMost(23), minute)
    }
}
