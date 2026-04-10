package com.giwon.assistant.features.briefing.service

import com.giwon.assistant.features.briefing.dto.CalendarItem
import com.giwon.assistant.features.briefing.dto.HeadlineItem
import com.giwon.assistant.features.briefing.dto.TaskItem
import com.giwon.assistant.features.briefing.dto.WeatherSummary

data class BriefingSummaryRequest(
    val weather: WeatherSummary,
    val calendar: List<CalendarItem>,
    val headlines: List<HeadlineItem>,
    val tasks: List<TaskItem>,
)

data class BriefingSummaryResult(
    val summary: String,
    val focusSuggestion: String,
)

fun interface BriefingSummaryProvider {
    fun summarize(request: BriefingSummaryRequest): BriefingSummaryResult
}
