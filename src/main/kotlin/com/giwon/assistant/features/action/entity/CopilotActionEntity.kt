package com.giwon.assistant.features.action.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.OffsetDateTime

@Entity
@Table(name = "copilot_action")
class CopilotActionEntity(
    @Id
    @Column(name = "id", nullable = false, length = 64)
    var id: String = "",
    @Column(name = "title", nullable = false, columnDefinition = "text")
    var title: String = "",
    @Column(name = "source_question", nullable = false, columnDefinition = "text")
    var sourceQuestion: String = "",
    @Column(name = "status", nullable = false, length = 32)
    var status: String = "",
    @Column(name = "priority", nullable = false, length = 16)
    var priority: String = "MEDIUM",
    @Column(name = "due_date")
    var dueDate: OffsetDateTime? = null,
    @Column(name = "created_at", nullable = false)
    var createdAt: OffsetDateTime = OffsetDateTime.now(),
    @Column(name = "updated_at", nullable = false)
    var updatedAt: OffsetDateTime = OffsetDateTime.now(),
    @Column(name = "completed_at")
    var completedAt: OffsetDateTime? = null,
)
