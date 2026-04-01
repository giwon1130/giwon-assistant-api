package com.giwon.assistant.features.idea.controller

import com.giwon.assistant.common.ApiResponse
import com.giwon.assistant.features.idea.dto.CreateIdeaRequest
import com.giwon.assistant.features.idea.dto.IdeaDetailResponse
import com.giwon.assistant.features.idea.dto.IdeaSummaryRequest
import com.giwon.assistant.features.idea.dto.IdeaSummaryResponse
import com.giwon.assistant.features.idea.dto.UpdateIdeaRequest
import com.giwon.assistant.features.idea.service.IdeaService
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/ideas")
class IdeaController(
    private val ideaService: IdeaService,
) {
    @PostMapping
    fun create(@Valid @RequestBody request: CreateIdeaRequest): ApiResponse<IdeaDetailResponse> =
        ApiResponse.ok(ideaService.create(request))

    @GetMapping
    fun getAll(): ApiResponse<List<IdeaDetailResponse>> =
        ApiResponse.ok(ideaService.getAll())

    @GetMapping("/{id}")
    fun getById(@PathVariable id: String): ApiResponse<IdeaDetailResponse> =
        ApiResponse.ok(ideaService.getById(id))

    @PatchMapping("/{id}")
    fun update(
        @PathVariable id: String,
        @RequestBody request: UpdateIdeaRequest,
    ): ApiResponse<IdeaDetailResponse> =
        ApiResponse.ok(ideaService.update(id, request))

    @PostMapping("/summaries")
    fun summarize(@Valid @RequestBody request: IdeaSummaryRequest): ApiResponse<IdeaSummaryResponse> =
        ApiResponse.ok(ideaService.summarize(request))
}
