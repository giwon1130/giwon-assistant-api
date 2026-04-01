package com.giwon.assistant.features.idea.dto

data class IdeaSummaryResponse(
    val title: String,
    val summary: String,
    val keyPoints: List<String>,
    val suggestedActions: List<String>,
)
