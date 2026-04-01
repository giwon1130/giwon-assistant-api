package com.giwon.assistant.features.briefing.service

import com.giwon.assistant.features.briefing.dto.CalendarItem
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
@ConditionalOnProperty(
    prefix = "assistant.integrations",
    name = ["calendar-enabled"],
    havingValue = "true",
)
class GoogleCalendarProvider(
    private val calendarProperties: AssistantCalendarProperties,
) : CalendarProvider {
    override fun getEvents(date: LocalDate): List<CalendarItem> {
        // 실제 Google Calendar 연동 전까지는 provider 구조만 먼저 유지한다.
        return calendarProperties.defaultEvents.map { event ->
            CalendarItem(
                time = event.time,
                title = event.title,
            )
        }
    }
}
