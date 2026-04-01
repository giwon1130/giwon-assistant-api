package com.giwon.assistant.features.idea.dto

import jakarta.validation.constraints.NotBlank

data class IdeaSummaryRequest(
    val title: String? = null,
    @field:NotBlank
    val rawText: String,
)
