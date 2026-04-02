package com.giwon.assistant.features.briefing.service

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.giwon.assistant.features.briefing.dto.BriefingHistoryResponse
import com.giwon.assistant.features.briefing.dto.BriefingScheduleStatusResponse
import com.giwon.assistant.features.briefing.dto.CalendarItem
import com.giwon.assistant.features.briefing.dto.HeadlineItem
import com.giwon.assistant.features.briefing.dto.TaskItem
import com.giwon.assistant.features.briefing.dto.TodayBriefingResponse
import com.giwon.assistant.features.briefing.dto.WeatherSummary
import com.giwon.assistant.features.briefing.entity.BriefingHistoryEntity
import com.giwon.assistant.features.briefing.repository.BriefingHistoryRepository
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.util.UUID

@Service
class BriefingService(
    private val briefingHistoryRepository: BriefingHistoryRepository,
    private val objectMapper: ObjectMapper,
    private val scheduleProperties: AssistantBriefingScheduleProperties,
) {
    companion object {
        private const val MANUAL_SOURCE = "MANUAL"
        private const val AUTOMATED_SOURCE = "AUTOMATED"
    }

    private fun fallbackWeather(): WeatherSummary =
        WeatherSummary(
            location = "Seoul",
            condition = "맑음",
            temperatureCelsius = 18,
        )

    fun getTodayBriefing(
        weatherProvider: WeatherProvider,
        calendarProvider: CalendarProvider,
        newsProvider: NewsProvider,
    ): TodayBriefingResponse =
        buildAndSaveBriefing(
            weatherProvider = weatherProvider,
            calendarProvider = calendarProvider,
            newsProvider = newsProvider,
            source = MANUAL_SOURCE,
        )

    fun generateScheduledBriefing(
        weatherProvider: WeatherProvider,
        calendarProvider: CalendarProvider,
        newsProvider: NewsProvider,
    ): TodayBriefingResponse? {
        val zoneId = ZoneId.of(scheduleProperties.zone)
        val today = LocalDate.now(zoneId)
        val start = today.atStartOfDay(zoneId).toOffsetDateTime()
        val end = today.atTime(LocalTime.MAX).atZone(zoneId).toOffsetDateTime()

        if (briefingHistoryRepository.existsBySourceAndGeneratedAtBetween(AUTOMATED_SOURCE, start, end)) {
            return null
        }

        return buildAndSaveBriefing(
            weatherProvider = weatherProvider,
            calendarProvider = calendarProvider,
            newsProvider = newsProvider,
            source = AUTOMATED_SOURCE,
        )
    }

    fun getScheduleStatus(): BriefingScheduleStatusResponse =
        BriefingScheduleStatusResponse(
            enabled = scheduleProperties.enabled,
            cron = scheduleProperties.cron,
            zone = scheduleProperties.zone,
            lastAutomatedBriefingAt = briefingHistoryRepository.findTop1BySourceOrderByGeneratedAtDesc(AUTOMATED_SOURCE)
                ?.generatedAt
                ?.toString(),
        )

    private fun buildAndSaveBriefing(
        weatherProvider: WeatherProvider,
        calendarProvider: CalendarProvider,
        newsProvider: NewsProvider,
        source: String,
    ): TodayBriefingResponse {
        val response = TodayBriefingResponse(
            generatedAt = OffsetDateTime.now().toString(),
            summary = "오늘은 오전 집중 작업 1건과 오후 미팅 1건이 있어. 먼저 중요한 작업을 끝내는 흐름이 좋다.",
            weather = runCatching { weatherProvider.getCurrentWeather() }.getOrElse { fallbackWeather() },
            calendar = runCatching { calendarProvider.getEvents(LocalDate.now()) }
                .getOrElse {
                    listOf(
                        CalendarItem(time = "10:00", title = "프로젝트 구조 정리"),
                        CalendarItem(time = "15:00", title = "개인 서비스 점검"),
                    )
                },
            headlines = runCatching { newsProvider.getTopHeadlines() }
                .getOrElse {
                    listOf(
                        HeadlineItem(source = "Tech", title = "AI 제품화 경쟁이 심화되는 중"),
                        HeadlineItem(source = "Local", title = "날씨 변동 폭이 커 외출 전 확인 필요"),
                    )
                },
            tasks = listOf(
                TaskItem(priority = "HIGH", title = "AI 비서 MVP API 구조 확정"),
                TaskItem(priority = "MEDIUM", title = "giwon-home에 live 링크 연결"),
                TaskItem(priority = "LOW", title = "아이디어 노트 정리"),
            ),
            focusSuggestion = "오전에는 설계와 구현을 한 번에 끝내고, 오후에는 연결 작업과 정리에 집중하는 게 좋다.",
        )

        briefingHistoryRepository.save(response.toEntity(source))
        return response
    }

    fun getRecentHistory(): List<BriefingHistoryResponse> =
        briefingHistoryRepository.findTop7ByOrderByGeneratedAtDesc().map { it.toResponse() }

    private fun TodayBriefingResponse.toEntity(source: String): BriefingHistoryEntity =
        BriefingHistoryEntity(
            id = "BRIEFING-${UUID.randomUUID()}",
            generatedAt = OffsetDateTime.parse(generatedAt),
            source = source,
            summary = summary,
            weatherLocation = weather.location,
            weatherCondition = weather.condition,
            weatherTemperatureCelsius = weather.temperatureCelsius,
            calendarItems = objectMapper.writeValueAsString(calendar),
            headlines = objectMapper.writeValueAsString(headlines),
            tasks = objectMapper.writeValueAsString(tasks),
            focusSuggestion = focusSuggestion,
        )

    private fun BriefingHistoryEntity.toResponse(): BriefingHistoryResponse =
        BriefingHistoryResponse(
            id = id,
            generatedAt = generatedAt.toString(),
            summary = summary,
            weather = WeatherSummary(
                location = weatherLocation,
                condition = weatherCondition,
                temperatureCelsius = weatherTemperatureCelsius,
            ),
            calendar = objectMapper.readValue(calendarItems, object : TypeReference<List<CalendarItem>>() {}),
            headlines = objectMapper.readValue(headlines, object : TypeReference<List<HeadlineItem>>() {}),
            tasks = objectMapper.readValue(tasks, object : TypeReference<List<TaskItem>>() {}),
            focusSuggestion = focusSuggestion,
        )
}
