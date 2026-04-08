package com.giwon.assistant.features.checkin.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDate
import java.time.OffsetDateTime

@Entity
@Table(name = "daily_condition_checkin")
class DailyConditionCheckinEntity(
    @Id
    @Column(name = "check_date", nullable = false)
    var checkDate: LocalDate = LocalDate.now(),
    @Column(name = "energy", nullable = false)
    var energy: Int = 3,
    @Column(name = "focus", nullable = false)
    var focus: Int = 3,
    @Column(name = "mood", nullable = false)
    var mood: Int = 3,
    @Column(name = "stress", nullable = false)
    var stress: Int = 3,
    @Column(name = "sleep_quality", nullable = false)
    var sleepQuality: Int = 3,
    @Column(name = "note", columnDefinition = "text")
    var note: String? = null,
    @Column(name = "updated_at", nullable = false)
    var updatedAt: OffsetDateTime = OffsetDateTime.now(),
)
