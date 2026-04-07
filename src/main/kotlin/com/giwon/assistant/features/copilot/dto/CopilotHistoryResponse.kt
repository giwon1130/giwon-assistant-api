package com.giwon.assistant.features.copilot.dto

data class CopilotHistoryResponse(
    val id: String,
    val question: String,
    val answer: String,
    val reasoning: List<String>,
    val suggestedActions: List<String>,
    val source: String,
    val generatedAt: String,
)
