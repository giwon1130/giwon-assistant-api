package com.giwon.assistant.features.action.dto

import jakarta.validation.constraints.NotBlank

data class CreateCopilotActionRequest(
    @field:NotBlank
    val title: String,
    @field:NotBlank
    val sourceQuestion: String,
)
