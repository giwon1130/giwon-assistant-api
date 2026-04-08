package com.giwon.assistant.features.review.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.OffsetDateTime

@Entity
@Table(name = "weekly_review_snapshot")
class WeeklyReviewSnapshotEntity(
    @Id
    @Column(name = "id", nullable = false, length = 64)
    var id: String = "",
    @Column(name = "period_start", nullable = false)
    var periodStart: OffsetDateTime = OffsetDateTime.now(),
    @Column(name = "period_end", nullable = false)
    var periodEnd: OffsetDateTime = OffsetDateTime.now(),
    @Column(name = "summary", nullable = false, columnDefinition = "text")
    var summary: String = "",
    @Column(name = "metrics", nullable = false, columnDefinition = "text")
    var metrics: String = "",
    @Column(name = "wins", nullable = false, columnDefinition = "text")
    var wins: String = "[]",
    @Column(name = "risks", nullable = false, columnDefinition = "text")
    var risks: String = "[]",
    @Column(name = "next_focus", nullable = false, columnDefinition = "text")
    var nextFocus: String = "[]",
    @Column(name = "generated_at", nullable = false)
    var generatedAt: OffsetDateTime = OffsetDateTime.now(),
)
