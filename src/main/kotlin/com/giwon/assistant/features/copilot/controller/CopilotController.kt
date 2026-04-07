package com.giwon.assistant.features.copilot.controller

import com.giwon.assistant.common.ApiResponse
import com.giwon.assistant.features.copilot.dto.TodayCopilotResponse
import com.giwon.assistant.features.copilot.service.CopilotService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/copilot")
class CopilotController(
    private val copilotService: CopilotService,
) {
    @GetMapping("/today")
    fun getTodayCopilot(): ApiResponse<TodayCopilotResponse> =
        ApiResponse.ok(copilotService.getTodayCopilot())
}
