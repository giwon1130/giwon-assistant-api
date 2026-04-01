package com.giwon.assistant.features.planner.dto

data class TodayPlanResponse(
    val date: String,
    val topPriorities: List<String>,
    val timeBlocks: List<TimeBlock>,
    val reminders: List<String>,
)

data class TimeBlock(
    val start: String,
    val end: String,
    val activity: String,
)
