package com.giwon.assistant.features.idea.dto

data class IdeaDetailResponse(
    val id: String,
    val title: String,
    val rawText: String,
    val summary: String,
    val keyPoints: List<String>,
    val suggestedActions: List<String>,
    val tags: List<String>,
    val status: String,
    val createdAt: String,
    val updatedAt: String,
)
