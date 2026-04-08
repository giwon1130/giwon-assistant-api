package com.giwon.assistant.features.checkin.controller

import com.giwon.assistant.common.ApiResponse
import com.giwon.assistant.features.checkin.dto.DailyConditionCheckinResponse
import com.giwon.assistant.features.checkin.dto.UpdateDailyConditionCheckinRequest
import com.giwon.assistant.features.checkin.service.DailyConditionCheckinService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/checkins")
class DailyConditionCheckinController(
    private val dailyConditionCheckinService: DailyConditionCheckinService,
) {
    @GetMapping("/condition")
    fun getCondition(
        @RequestParam(required = false) date: String?,
    ): ApiResponse<DailyConditionCheckinResponse> =
        ApiResponse.ok(dailyConditionCheckinService.getCondition(date))

    @PatchMapping("/condition")
    fun updateCondition(
        @RequestBody request: UpdateDailyConditionCheckinRequest,
        @RequestParam(required = false) date: String?,
    ): ApiResponse<DailyConditionCheckinResponse> =
        ApiResponse.ok(dailyConditionCheckinService.updateCondition(date, request))
}
