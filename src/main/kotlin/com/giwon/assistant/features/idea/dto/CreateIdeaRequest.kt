package com.giwon.assistant.features.idea.dto

import jakarta.validation.constraints.NotBlank

data class CreateIdeaRequest(
    @field:NotBlank
    val title: String,
    @field:NotBlank
    val rawText: String,
    val tags: List<String> = emptyList(),
)
