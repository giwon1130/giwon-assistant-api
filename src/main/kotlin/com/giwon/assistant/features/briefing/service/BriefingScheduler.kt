package com.giwon.assistant.features.briefing.service

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(
    prefix = "assistant.briefing-schedule",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = true,
)
class BriefingScheduler(
    private val briefingService: BriefingService,
    private val weatherProvider: WeatherProvider,
    private val calendarProvider: CalendarProvider,
    private val newsProvider: NewsProvider,
) {
    @Scheduled(
        cron = "\${assistant.briefing-schedule.cron:0 0 8 * * *}",
        zone = "\${assistant.briefing-schedule.zone:Asia/Seoul}",
    )
    fun generateMorningBriefing() {
        briefingService.generateScheduledBriefing(
            weatherProvider = weatherProvider,
            calendarProvider = calendarProvider,
            newsProvider = newsProvider,
        )
    }
}
