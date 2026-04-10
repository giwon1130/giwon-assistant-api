package com.giwon.assistant.features.copilot.repository

import com.giwon.assistant.features.copilot.entity.CopilotHistoryEntity
import org.springframework.data.jpa.repository.JpaRepository

interface CopilotHistoryRepository : JpaRepository<CopilotHistoryEntity, String> {
    fun findTop10ByOrderByGeneratedAtDesc(): List<CopilotHistoryEntity>
    fun findTop3ByRatingOrderByGeneratedAtDesc(rating: Int): List<CopilotHistoryEntity>
}
