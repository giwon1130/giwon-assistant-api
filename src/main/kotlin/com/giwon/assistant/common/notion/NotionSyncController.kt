package com.giwon.assistant.common.notion

import com.giwon.assistant.common.ApiResponse
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/sync")
class NotionSyncController(
    private val notionIdeaSyncService: NotionIdeaSyncService,
) {
    @PostMapping("/notion")
    fun syncFromNotion(): ApiResponse<Unit> {
        notionIdeaSyncService.syncFromNotion()
        return ApiResponse.ok(Unit)
    }
}
