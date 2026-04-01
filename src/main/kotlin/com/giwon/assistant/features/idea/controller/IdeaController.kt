package com.giwon.assistant.features.idea.controller

import com.giwon.assistant.common.ApiResponse
import com.giwon.assistant.features.idea.dto.IdeaSummaryRequest
import com.giwon.assistant.features.idea.dto.IdeaSummaryResponse
import com.giwon.assistant.features.idea.service.IdeaService
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/ideas")
class IdeaController(
    private val ideaService: IdeaService,
) {
    @PostMapping("/summaries")
    fun summarize(@Valid @RequestBody request: IdeaSummaryRequest): ApiResponse<IdeaSummaryResponse> =
        ApiResponse.ok(ideaService.summarize(request))
}
