package com.giwon.assistant.features.copilot.controller

import com.giwon.assistant.common.ApiResponse
import com.giwon.assistant.features.copilot.dto.CopilotAskRequest
import com.giwon.assistant.features.copilot.dto.CopilotAskResponse
import com.giwon.assistant.features.copilot.dto.CopilotHistoryResponse
import com.giwon.assistant.features.copilot.dto.TodayCopilotResponse
import com.giwon.assistant.features.copilot.service.CopilotService
import com.giwon.assistant.features.copilot.service.CopilotStreamService
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

@RestController
@RequestMapping("/api/v1/copilot")
class CopilotController(
    private val copilotService: CopilotService,
    private val copilotStreamService: CopilotStreamService,
) {
    @GetMapping("/today")
    fun getTodayCopilot(): ApiResponse<TodayCopilotResponse> =
        ApiResponse.ok(copilotService.getTodayCopilot())

    @GetMapping("/history")
    fun getHistory(): ApiResponse<List<CopilotHistoryResponse>> =
        ApiResponse.ok(copilotService.getRecentHistory())

    @PostMapping("/ask")
    fun ask(@Valid @RequestBody request: CopilotAskRequest): ApiResponse<CopilotAskResponse> =
        ApiResponse.ok(copilotService.ask(request.question))

    @PostMapping("/ask/stream")
    fun askStream(@Valid @RequestBody request: CopilotAskRequest): SseEmitter =
        copilotStreamService.askStream(request.question)

    @PostMapping("/history/{id}/rating")
    fun rateHistory(
        @PathVariable id: String,
        @RequestParam rating: Int,
    ): ApiResponse<Unit> {
        require(rating == 1 || rating == -1) { "rating must be 1 (good) or -1 (bad)" }
        copilotService.rateHistory(id, rating)
        return ApiResponse.ok(Unit)
    }
}
