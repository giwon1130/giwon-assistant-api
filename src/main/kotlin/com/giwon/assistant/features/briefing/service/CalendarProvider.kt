package com.giwon.assistant.features.briefing.service

import com.giwon.assistant.features.briefing.dto.CalendarItem
import java.time.LocalDate

fun interface CalendarProvider {
    fun getEvents(date: LocalDate): List<CalendarItem>
}
