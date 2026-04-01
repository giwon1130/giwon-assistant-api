package com.giwon.assistant.features.idea.entity

import com.giwon.assistant.common.ListStringJsonConverter
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.OffsetDateTime

@Entity
@Table(name = "idea")
class IdeaEntity(
    @Id
    @Column(name = "id", nullable = false, length = 64)
    var id: String = "",
    @Column(name = "title", nullable = false)
    var title: String = "",
    @Column(name = "raw_text", nullable = false, columnDefinition = "text")
    var rawText: String = "",
    @Column(name = "summary", nullable = false, columnDefinition = "text")
    var summary: String = "",
    @Convert(converter = ListStringJsonConverter::class)
    @Column(name = "key_points", nullable = false, columnDefinition = "text")
    var keyPoints: List<String> = emptyList(),
    @Convert(converter = ListStringJsonConverter::class)
    @Column(name = "suggested_actions", nullable = false, columnDefinition = "text")
    var suggestedActions: List<String> = emptyList(),
    @Convert(converter = ListStringJsonConverter::class)
    @Column(name = "tags", nullable = false, columnDefinition = "text")
    var tags: List<String> = emptyList(),
    @Column(name = "status", nullable = false, length = 64)
    var status: String = "",
    @Column(name = "created_at", nullable = false)
    var createdAt: OffsetDateTime = OffsetDateTime.now(),
    @Column(name = "updated_at", nullable = false)
    var updatedAt: OffsetDateTime = OffsetDateTime.now(),
)
