package com.giwon.assistant.features.review.controller

import com.giwon.assistant.common.ApiResponse
import com.giwon.assistant.features.review.dto.WeeklyReviewResponse
import com.giwon.assistant.features.review.dto.WeeklyReviewSnapshotResponse
import com.giwon.assistant.features.review.service.WeeklyReviewService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/reviews")
class WeeklyReviewController(
    private val weeklyReviewService: WeeklyReviewService,
) {
    @GetMapping("/weekly")
    fun getWeeklyReview(): ApiResponse<WeeklyReviewResponse> =
        ApiResponse.ok(weeklyReviewService.getWeeklyReview())

    @GetMapping("/weekly/history")
    fun getWeeklyReviewHistory(): ApiResponse<List<WeeklyReviewSnapshotResponse>> =
        ApiResponse.ok(weeklyReviewService.getWeeklyReviewHistory())
}
