package com.giwon.assistant.features.review.repository

import com.giwon.assistant.features.review.entity.WeeklyReviewSnapshotEntity
import org.springframework.data.jpa.repository.JpaRepository

interface WeeklyReviewSnapshotRepository : JpaRepository<WeeklyReviewSnapshotEntity, String> {
    fun findTop8ByOrderByGeneratedAtDesc(): List<WeeklyReviewSnapshotEntity>
}
