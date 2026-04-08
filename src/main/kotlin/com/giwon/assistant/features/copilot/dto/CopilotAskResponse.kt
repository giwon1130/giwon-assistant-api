package com.giwon.assistant.features.copilot.dto

data class CopilotAskResponse(
    val question: String,
    val answer: String,
    val intent: String,
    val reasoning: List<String>,
    val suggestedActions: List<String>,
    val suggestedActionPlans: List<CopilotSuggestedActionPlan>,
    val source: String,
    val fallbackReason: String? = null,
    val generatedAt: String,
)
