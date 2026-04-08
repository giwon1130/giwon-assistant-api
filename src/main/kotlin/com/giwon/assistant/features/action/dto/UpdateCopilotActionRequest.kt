package com.giwon.assistant.features.action.dto

data class UpdateCopilotActionRequest(
    val title: String? = null,
    val priority: String? = null,
    val dueDate: String? = null,
)
