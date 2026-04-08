package com.giwon.assistant.features.routine.controller

import com.giwon.assistant.common.ApiResponse
import com.giwon.assistant.features.routine.dto.DailyRoutineResponse
import com.giwon.assistant.features.routine.dto.UpdateDailyRoutineRequest
import com.giwon.assistant.features.routine.service.DailyRoutineService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/routines")
class DailyRoutineController(
    private val dailyRoutineService: DailyRoutineService,
) {
    @GetMapping("/daily")
    fun getDailyRoutine(
        @RequestParam(required = false) date: String?,
    ): ApiResponse<DailyRoutineResponse> =
        ApiResponse.ok(dailyRoutineService.getDailyRoutine(date))

    @PatchMapping("/daily/{itemKey}")
    fun updateDailyRoutine(
        @PathVariable itemKey: String,
        @RequestBody request: UpdateDailyRoutineRequest,
        @RequestParam(required = false) date: String?,
    ): ApiResponse<DailyRoutineResponse> =
        ApiResponse.ok(dailyRoutineService.updateDailyRoutine(date, itemKey, request))
}
