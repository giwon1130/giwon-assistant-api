package com.giwon.assistant.features.checkin.dto

data class DailyConditionCheckinResponse(
    val date: String,
    val energy: Int,
    val focus: Int,
    val mood: Int,
    val stress: Int,
    val sleepQuality: Int,
    val note: String?,
    val readinessScore: Int,
    val trend: String,
    val summary: String,
    val suggestions: List<String>,
    val recentReadiness: List<DailyConditionReadinessPointResponse>,
)

data class DailyConditionReadinessPointResponse(
    val date: String,
    val readinessScore: Int,
)
