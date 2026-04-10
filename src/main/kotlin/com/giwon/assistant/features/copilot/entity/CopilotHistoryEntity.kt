package com.giwon.assistant.features.copilot.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.OffsetDateTime

@Entity
@Table(name = "copilot_history")
class CopilotHistoryEntity(
    @Id
    @Column(name = "id", nullable = false, length = 64)
    var id: String = "",
    @Column(name = "generated_at", nullable = false)
    var generatedAt: OffsetDateTime = OffsetDateTime.now(),
    @Column(name = "question", nullable = false, columnDefinition = "text")
    var question: String = "",
    @Column(name = "answer", nullable = false, columnDefinition = "text")
    var answer: String = "",
    @Column(name = "reasoning", nullable = false, columnDefinition = "text")
    var reasoning: String = "[]",
    @Column(name = "suggested_actions", nullable = false, columnDefinition = "text")
    var suggestedActions: String = "[]",
    @Column(name = "source", nullable = false, length = 32)
    var source: String = "",
    @Column(name = "rating")
    var rating: Int? = null,
)
