package com.giwon.assistant.features.copilot.dto

data class TodayCopilotResponse(
    val generatedAt: String,
    val headline: String,
    val overview: String,
    val topPriority: String,
    val suggestedNextAction: String,
    val routineSummary: String,
    val routineSuggestedAction: String,
    val conditionSummary: String,
    val conditionSuggestedAction: String,
    val conditionReadinessScore: Int,
    val risks: List<String>,
    val recommendedIdeas: List<CopilotIdeaAction>,
    val todayFlow: List<CopilotTimeSuggestion>,
)

data class CopilotIdeaAction(
    val id: String,
    val title: String,
    val status: String,
    val recommendedAction: String,
)

data class CopilotTimeSuggestion(
    val time: String,
    val focus: String,
    val reason: String,
)
