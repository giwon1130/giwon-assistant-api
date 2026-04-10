package com.giwon.assistant.features.briefing.dto

data class TodayBriefingResponse(
    val generatedAt: String,
    val summary: String,
    val weather: WeatherSummary,
    val calendar: List<CalendarItem>,
    val headlines: List<HeadlineItem>,
    val tasks: List<TaskItem>,
    val focusSuggestion: String,
)

data class WeatherSummary(
    val location: String,
    val condition: String,
    val temperatureCelsius: Int,
)

data class CalendarItem(
    val time: String,
    val title: String,
    val mock: Boolean = false,
)

data class HeadlineItem(
    val source: String,
    val title: String,
    val mock: Boolean = false,
)

data class TaskItem(
    val priority: String,
    val title: String,
)
