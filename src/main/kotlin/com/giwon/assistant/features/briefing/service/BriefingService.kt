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
import com.giwon.assistant.common.notion.NotionBriefingExporter
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
    private val notionBriefingExporter: NotionBriefingExporter,
    private val briefingSummaryProvider: BriefingSummaryProvider,
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
        val weather = runCatching { weatherProvider.getCurrentWeather() }.getOrElse { fallbackWeather() }
        val calendar = runCatching { calendarProvider.getEvents(LocalDate.now()) }
            .getOrElse {
                listOf(
                    CalendarItem(time = "10:00", title = "프로젝트 구조 정리", mock = true),
                    CalendarItem(time = "15:00", title = "개인 서비스 점검", mock = true),
                )
            }
        val headlines = runCatching { newsProvider.getTopHeadlines() }
            .getOrElse {
                listOf(
                    HeadlineItem(source = "Tech", title = "AI 제품화 경쟁이 심화되는 중", mock = true),
                    HeadlineItem(source = "Local", title = "날씨 변동 폭이 커 외출 전 확인 필요", mock = true),
                )
            }
        val tasks = listOf(
            TaskItem(priority = "HIGH", title = "AI 비서 MVP API 구조 확정"),
            TaskItem(priority = "MEDIUM", title = "giwon-home에 live 링크 연결"),
            TaskItem(priority = "LOW", title = "아이디어 노트 정리"),
        )

        val summaryResult = runCatching {
            briefingSummaryProvider.summarize(
                BriefingSummaryRequest(
                    weather = weather,
                    calendar = calendar,
                    headlines = headlines,
                    tasks = tasks,
                )
            )
        }.getOrElse {
            BriefingSummaryResult(
                summary = "오늘 하루 브리핑 요약을 생성하지 못했습니다. [목데이터]",
                focusSuggestion = "중요한 작업에 먼저 집중하세요. [목데이터]",
            )
        }

        val response = TodayBriefingResponse(
            generatedAt = OffsetDateTime.now().toString(),
            summary = summaryResult.summary,
            weather = weather,
            calendar = calendar,
            headlines = headlines,
            tasks = tasks,
            focusSuggestion = summaryResult.focusSuggestion,
        )

        briefingHistoryRepository.save(response.toEntity(source))
        notionBriefingExporter.export(response, source)
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
