package com.giwon.assistant.features.review.service

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class WeeklyReviewScheduler(
    private val weeklyReviewService: WeeklyReviewService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    // 매주 월요일 오전 9시 (Asia/Seoul)
    @Scheduled(cron = "0 0 9 * * MON", zone = "Asia/Seoul")
    fun generateWeeklyReview() {
        log.info("Weekly review scheduler triggered")
        runCatching {
            weeklyReviewService.getWeeklyReview()
            log.info("Weekly review generated successfully")
        }.onFailure {
            log.error("Failed to generate weekly review: ${it.message}")
        }
    }
}
