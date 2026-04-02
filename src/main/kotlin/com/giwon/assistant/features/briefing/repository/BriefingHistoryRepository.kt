package com.giwon.assistant.features.briefing.repository

import com.giwon.assistant.features.briefing.entity.BriefingHistoryEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.time.OffsetDateTime

interface BriefingHistoryRepository : JpaRepository<BriefingHistoryEntity, String> {
    fun findTop7ByOrderByGeneratedAtDesc(): List<BriefingHistoryEntity>
    fun findTop1BySourceOrderByGeneratedAtDesc(source: String): BriefingHistoryEntity?
    fun existsBySourceAndGeneratedAtBetween(source: String, start: OffsetDateTime, end: OffsetDateTime): Boolean
}
