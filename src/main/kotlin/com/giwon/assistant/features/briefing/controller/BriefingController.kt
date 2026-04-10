package com.giwon.assistant.features.briefing.controller

import com.giwon.assistant.common.ApiResponse
import com.giwon.assistant.features.briefing.dto.BriefingHistoryResponse
import com.giwon.assistant.features.briefing.dto.BriefingScheduleStatusResponse
import com.giwon.assistant.features.briefing.dto.TodayBriefingResponse
import com.giwon.assistant.features.briefing.service.BriefingAudioService
import com.giwon.assistant.features.briefing.service.BriefingService
import com.giwon.assistant.features.briefing.service.CalendarProvider
import com.giwon.assistant.features.briefing.service.NewsProvider
import com.giwon.assistant.features.briefing.service.WeatherProvider
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/briefings")
class BriefingController(
    private val briefingService: BriefingService,
    private val weatherProvider: WeatherProvider,
    private val calendarProvider: CalendarProvider,
    private val newsProvider: NewsProvider,
    private val briefingAudioService: BriefingAudioService,
) {
    @GetMapping("/today")
    fun getTodayBriefing(): ApiResponse<TodayBriefingResponse> =
        ApiResponse.ok(briefingService.getTodayBriefing(weatherProvider, calendarProvider, newsProvider))

    @GetMapping("/history")
    fun getRecentHistory(): ApiResponse<List<BriefingHistoryResponse>> =
        ApiResponse.ok(briefingService.getRecentHistory())

    @GetMapping("/schedule")
    fun getScheduleStatus(): ApiResponse<BriefingScheduleStatusResponse> =
        ApiResponse.ok(briefingService.getScheduleStatus())

    @GetMapping("/audio", produces = ["audio/mpeg"])
    fun getAudio(): ResponseEntity<ByteArray> {
        val audio = briefingAudioService.generateAudio()
            ?: return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build()
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"briefing.mp3\"")
            .contentType(MediaType.parseMediaType("audio/mpeg"))
            .body(audio)
    }
}
