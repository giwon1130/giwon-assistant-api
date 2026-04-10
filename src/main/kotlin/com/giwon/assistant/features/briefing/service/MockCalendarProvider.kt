package com.giwon.assistant.features.briefing.service

import com.giwon.assistant.features.briefing.dto.CalendarItem
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
@ConditionalOnProperty(
    prefix = "assistant.integrations",
    name = ["calendar-enabled"],
    havingValue = "false",
    matchIfMissing = true,
)
class MockCalendarProvider(
    private val calendarProperties: AssistantCalendarProperties,
) : CalendarProvider {
    override fun getEvents(date: LocalDate): List<CalendarItem> =
        calendarProperties.defaultEvents.map { event ->
            CalendarItem(
                time = event.time,
                title = event.title,
                mock = true,
            )
        }
}
