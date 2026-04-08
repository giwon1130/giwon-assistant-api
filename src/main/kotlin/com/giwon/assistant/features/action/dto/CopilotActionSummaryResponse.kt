package com.giwon.assistant.features.action.dto

data class CopilotActionSummaryResponse(
    val totalCount: Int,
    val openCount: Int,
    val doneCount: Int,
    val overdueCount: Int,
    val dueSoonCount: Int,
    val highPriorityOpenCount: Int,
    val completionRate: Int,
)
