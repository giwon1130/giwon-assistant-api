package com.giwon.assistant.features.briefing.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.OffsetDateTime

@Entity
@Table(name = "briefing_history")
class BriefingHistoryEntity(
    @Id
    @Column(name = "id", nullable = false, length = 64)
    var id: String = "",
    @Column(name = "generated_at", nullable = false)
    var generatedAt: OffsetDateTime = OffsetDateTime.now(),
    @Column(name = "summary", nullable = false, columnDefinition = "text")
    var summary: String = "",
    @Column(name = "weather_location", nullable = false)
    var weatherLocation: String = "",
    @Column(name = "weather_condition", nullable = false)
    var weatherCondition: String = "",
    @Column(name = "weather_temperature_celsius", nullable = false)
    var weatherTemperatureCelsius: Int = 0,
    @Column(name = "calendar_items", nullable = false, columnDefinition = "text")
    var calendarItems: String = "[]",
    @Column(name = "headlines", nullable = false, columnDefinition = "text")
    var headlines: String = "[]",
    @Column(name = "tasks", nullable = false, columnDefinition = "text")
    var tasks: String = "[]",
    @Column(name = "focus_suggestion", nullable = false, columnDefinition = "text")
    var focusSuggestion: String = "",
)
