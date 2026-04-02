package com.giwon.assistant.features.briefing.dto

data class BriefingScheduleStatusResponse(
    val enabled: Boolean,
    val cron: String,
    val zone: String,
    val lastAutomatedBriefingAt: String?,
)
