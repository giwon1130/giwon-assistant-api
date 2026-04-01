package com.giwon.assistant.features.planner.service

import com.giwon.assistant.features.planner.dto.TimeBlock
import com.giwon.assistant.features.planner.dto.TodayPlanResponse
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class PlannerService {
    fun getTodayPlan(): TodayPlanResponse =
        TodayPlanResponse(
            date = LocalDate.now().toString(),
            topPriorities = listOf(
                "AI 비서 API MVP 확정",
                "허브 서비스 연동 정리",
                "아이디어 노트 구조화",
            ),
            timeBlocks = listOf(
                TimeBlock(start = "09:00", end = "11:00", activity = "집중 구현"),
                TimeBlock(start = "11:00", end = "12:00", activity = "아이디어 정리"),
                TimeBlock(start = "14:00", end = "16:00", activity = "연동 및 문서화"),
            ),
            reminders = listOf(
                "오후 작업 전에 브리핑 갱신",
                "새 아이디어는 요약 API로 먼저 정리",
            ),
        )
}
