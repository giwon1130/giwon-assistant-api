package com.giwon.assistant.features.review.dto

data class WeeklyReviewResponse(
    val periodStart: String,
    val periodEnd: String,
    val summary: String,
    val metrics: WeeklyReviewMetrics,
    val wins: List<String>,
    val risks: List<String>,
    val nextFocus: List<String>,
)

data class WeeklyReviewMetrics(
    val questionsAsked: Int,
    val actionsCreated: Int,
    val actionsCompleted: Int,
    val openActions: Int,
    val ideasCaptured: Int,
)
