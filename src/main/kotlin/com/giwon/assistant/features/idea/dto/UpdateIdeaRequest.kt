package com.giwon.assistant.features.idea.dto

data class UpdateIdeaRequest(
    val title: String? = null,
    val rawText: String? = null,
    val tags: List<String>? = null,
    val status: String? = null,
)
