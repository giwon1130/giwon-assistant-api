package com.giwon.assistant.features.planner.controller

import com.giwon.assistant.common.ApiResponse
import com.giwon.assistant.features.planner.dto.TodayPlanResponse
import com.giwon.assistant.features.planner.service.PlannerService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/plans")
class PlannerController(
    private val plannerService: PlannerService,
) {
    @GetMapping("/today")
    fun getTodayPlan(): ApiResponse<TodayPlanResponse> =
        ApiResponse.ok(plannerService.getTodayPlan())
}
