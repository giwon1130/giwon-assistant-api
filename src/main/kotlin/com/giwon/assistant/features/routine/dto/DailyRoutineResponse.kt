package com.giwon.assistant.features.routine.dto

data class DailyRoutineResponse(
    val date: String,
    val completionRate: Int,
    val completedCount: Int,
    val totalCount: Int,
    val streakDays: Int,
    val weeklyCompletionRate: Int,
    val weeklyCompletedDays: Int,
    val recentDays: List<DailyRoutineDaySummaryResponse>,
    val categoryStats: List<DailyRoutineCategoryStatResponse>,
    val items: List<DailyRoutineItemResponse>,
)

data class DailyRoutineDaySummaryResponse(
    val date: String,
    val completedCount: Int,
    val totalCount: Int,
    val completionRate: Int,
)

data class DailyRoutineCategoryStatResponse(
    val category: String,
    val completedCount: Int,
    val totalCount: Int,
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
