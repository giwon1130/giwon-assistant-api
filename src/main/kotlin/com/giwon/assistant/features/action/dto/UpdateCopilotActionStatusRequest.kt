package com.giwon.assistant.features.action.dto

import jakarta.validation.constraints.NotBlank

data class UpdateCopilotActionStatusRequest(
    @field:NotBlank
    val status: String,
)
