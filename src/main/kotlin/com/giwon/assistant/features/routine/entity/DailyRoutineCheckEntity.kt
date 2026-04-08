package com.giwon.assistant.features.routine.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDate
import java.time.OffsetDateTime

@Entity
@Table(name = "daily_routine_check")
class DailyRoutineCheckEntity(
    @Id
    @Column(name = "id", nullable = false, length = 64)
    var id: String = "",
    @Column(name = "check_date", nullable = false)
    var checkDate: LocalDate = LocalDate.now(),
    @Column(name = "item_key", nullable = false, length = 32)
    var itemKey: String = "",
    @Column(name = "completed", nullable = false)
    var completed: Boolean = false,
    @Column(name = "note", columnDefinition = "text")
    var note: String? = null,
    @Column(name = "completed_at")
    var completedAt: OffsetDateTime? = null,
    @Column(name = "updated_at", nullable = false)
    var updatedAt: OffsetDateTime = OffsetDateTime.now(),
)
