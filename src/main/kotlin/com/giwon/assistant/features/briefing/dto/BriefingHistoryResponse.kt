package com.giwon.assistant.features.briefing.dto

data class BriefingHistoryResponse(
    val id: String,
    val generatedAt: String,
    val summary: String,
    val weather: WeatherSummary,
    val calendar: List<CalendarItem>,
    val headlines: List<HeadlineItem>,
    val tasks: List<TaskItem>,
    val focusSuggestion: String,
)
