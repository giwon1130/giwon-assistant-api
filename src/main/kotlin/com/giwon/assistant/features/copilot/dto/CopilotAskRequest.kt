package com.giwon.assistant.features.copilot.dto

import jakarta.validation.constraints.NotBlank

data class CopilotAskRequest(
    @field:NotBlank
    val question: String,
)
