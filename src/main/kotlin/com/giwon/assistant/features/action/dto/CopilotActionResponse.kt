package com.giwon.assistant.features.action.dto

data class CopilotActionResponse(
    val id: String,
    val title: String,
    val sourceQuestion: String,
    val status: String,
    val createdAt: String,
    val updatedAt: String,
    val completedAt: String?,
)
