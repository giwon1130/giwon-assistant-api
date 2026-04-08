package com.giwon.assistant.features.review.dto

data class WeeklyReviewSnapshotResponse(
    val id: String,
    val periodStart: String,
    val periodEnd: String,
    val summary: String,
    val metrics: WeeklyReviewMetrics,
    val wins: List<String>,
    val risks: List<String>,
    val nextFocus: List<String>,
    val generatedAt: String,
)
