package com.giwon.assistant.features.checkin.dto

data class UpdateDailyConditionCheckinRequest(
    val energy: Int,
    val focus: Int,
    val mood: Int,
    val stress: Int,
    val sleepQuality: Int,
    val note: String? = null,
)
