package com.giwon.assistant.features.routine.dto

data class DailyRoutineResponse(
    val date: String,
    val completionRate: Int,
    val completedCount: Int,
    val totalCount: Int,
    val streakDays: Int,
    val weeklyCompletionRate: Int,
    val weeklyCompletedDays: Int,
    val energyScore: Int,
    val recoveryScore: Int,
    val riskLevel: String,
    val insight: String,
    val suggestedActions: List<String>,
    val signals: List<DailyRoutineSignalResponse>,
    val focusMode: DailyRoutineFocusModeResponse,
    val reminders: List<DailyRoutineReminderResponse>,
    val recentDays: List<DailyRoutineDaySummaryResponse>,
    val categoryStats: List<DailyRoutineCategoryStatResponse>,
    val items: List<DailyRoutineItemResponse>,
)

data class DailyRoutineSignalResponse(
    val label: String,
    val status: String,
    val detail: String,
)

data class DailyRoutineFocusModeResponse(
    val title: String,
    val durationMinutes: Int,
    val summary: String,
    val trigger: String,
)

data class DailyRoutineReminderResponse(
    val itemKey: String,
    val label: String,
    val reminderTime: String,
    val reason: String,
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
