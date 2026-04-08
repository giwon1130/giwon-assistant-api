package com.giwon.assistant.features.copilot.dto

data class CopilotSuggestedActionPlan(
    val title: String,
    val priority: String,
    val dueDate: String?,
    val dueLabel: String,
    val reason: String,
)
