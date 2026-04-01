package com.giwon.assistant.features.idea.model

import java.time.OffsetDateTime

data class Idea(
    val id: String,
    val title: String,
    val rawText: String,
    val summary: String,
    val keyPoints: List<String>,
    val suggestedActions: List<String>,
    val tags: List<String>,
    val status: String,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
)
