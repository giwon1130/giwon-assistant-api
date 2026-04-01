package com.giwon.assistant.features.briefing.controller

import com.giwon.assistant.common.ApiResponse
import com.giwon.assistant.features.briefing.dto.TodayBriefingResponse
import com.giwon.assistant.features.briefing.service.BriefingService
import com.giwon.assistant.features.briefing.service.CalendarProvider
import com.giwon.assistant.features.briefing.service.WeatherProvider
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/briefings")
class BriefingController(
    private val briefingService: BriefingService,
    private val weatherProvider: WeatherProvider,
    private val calendarProvider: CalendarProvider,
) {
    @GetMapping("/today")
    fun getTodayBriefing(): ApiResponse<TodayBriefingResponse> =
        ApiResponse.ok(briefingService.getTodayBriefing(weatherProvider, calendarProvider))
}
