package com.giwon.assistant.common.notion

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class NotionIdeaSyncScheduler(
    private val notionIdeaSyncService: NotionIdeaSyncService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    // 30분마다 Notion → API 상태 동기화
    @Scheduled(fixedDelay = 30 * 60 * 1000)
    fun syncFromNotion() {
        runCatching {
            notionIdeaSyncService.syncFromNotion()
        }.onFailure {
            log.error("Notion idea sync scheduler failed: ${it.message}")
        }
    }
}
