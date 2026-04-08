package com.giwon.assistant.features.routine.dto

data class UpdateDailyRoutineRequest(
    val completed: Boolean,
    val note: String? = null,
)
