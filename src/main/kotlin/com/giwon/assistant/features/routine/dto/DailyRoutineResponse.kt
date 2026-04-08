package com.giwon.assistant.features.routine.dto

data class DailyRoutineResponse(
    val date: String,
    val completionRate: Int,
    val completedCount: Int,
    val totalCount: Int,
    val streakDays: Int,
    val items: List<DailyRoutineItemResponse>,
)

data class DailyRoutineItemResponse(
    val key: String,
    val label: String,
    val description: String,
    val category: String,
    val targetTime: String,
    val completed: Boolean,
    val completedAt: String?,
    val note: String?,
)
