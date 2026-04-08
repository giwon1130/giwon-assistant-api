package com.giwon.assistant.features.action.controller

import com.giwon.assistant.common.ApiResponse
import com.giwon.assistant.features.action.dto.CopilotActionResponse
import com.giwon.assistant.features.action.dto.CreateCopilotActionRequest
import com.giwon.assistant.features.action.dto.UpdateCopilotActionStatusRequest
import com.giwon.assistant.features.action.service.CopilotActionService
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/actions")
class CopilotActionController(
    private val copilotActionService: CopilotActionService,
) {
    @PostMapping
    fun create(@Valid @RequestBody request: CreateCopilotActionRequest): ApiResponse<CopilotActionResponse> =
        ApiResponse.ok(copilotActionService.create(request))

    @GetMapping
    fun getAll(@RequestParam(required = false) status: String?): ApiResponse<List<CopilotActionResponse>> =
        ApiResponse.ok(copilotActionService.getAll(status))

    @PatchMapping("/{actionId}/status")
    fun updateStatus(
        @PathVariable actionId: String,
        @Valid @RequestBody request: UpdateCopilotActionStatusRequest,
    ): ApiResponse<CopilotActionResponse> =
        ApiResponse.ok(copilotActionService.updateStatus(actionId, request.status))
}
