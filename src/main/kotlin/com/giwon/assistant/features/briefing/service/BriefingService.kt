package com.giwon.assistant.features.briefing.service

import com.giwon.assistant.features.briefing.dto.CalendarItem
import com.giwon.assistant.features.briefing.dto.HeadlineItem
import com.giwon.assistant.features.briefing.dto.TaskItem
import com.giwon.assistant.features.briefing.dto.TodayBriefingResponse
import com.giwon.assistant.features.briefing.dto.WeatherSummary
import org.springframework.stereotype.Service
import java.time.OffsetDateTime

@Service
class BriefingService {
    private fun fallbackWeather(): WeatherSummary =
        WeatherSummary(
            location = "Seoul",
            condition = "맑음",
            temperatureCelsius = 18,
        )

    fun getTodayBriefing(weatherProvider: WeatherProvider): TodayBriefingResponse =
        TodayBriefingResponse(
            generatedAt = OffsetDateTime.now().toString(),
            summary = "오늘은 오전 집중 작업 1건과 오후 미팅 1건이 있어. 먼저 중요한 작업을 끝내는 흐름이 좋다.",
            weather = runCatching { weatherProvider.getCurrentWeather() }.getOrElse { fallbackWeather() },
            calendar = listOf(
                CalendarItem(time = "10:00", title = "프로젝트 구조 정리"),
                CalendarItem(time = "15:00", title = "개인 서비스 점검"),
            ),
            headlines = listOf(
                HeadlineItem(source = "Tech", title = "AI 제품화 경쟁이 심화되는 중"),
                HeadlineItem(source = "Local", title = "날씨 변동 폭이 커 외출 전 확인 필요"),
            ),
            tasks = listOf(
                TaskItem(priority = "HIGH", title = "AI 비서 MVP API 구조 확정"),
                TaskItem(priority = "MEDIUM", title = "giwon-home에 live 링크 연결"),
                TaskItem(priority = "LOW", title = "아이디어 노트 정리"),
            ),
            focusSuggestion = "오전에는 설계와 구현을 한 번에 끝내고, 오후에는 연결 작업과 정리에 집중하는 게 좋다.",
        )
}
