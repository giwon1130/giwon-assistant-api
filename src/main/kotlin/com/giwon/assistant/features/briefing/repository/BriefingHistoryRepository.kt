package com.giwon.assistant.features.briefing.repository

import com.giwon.assistant.features.briefing.entity.BriefingHistoryEntity
import org.springframework.data.jpa.repository.JpaRepository

interface BriefingHistoryRepository : JpaRepository<BriefingHistoryEntity, String> {
    fun findTop7ByOrderByGeneratedAtDesc(): List<BriefingHistoryEntity>
}
