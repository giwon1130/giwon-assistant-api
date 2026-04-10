package com.giwon.assistant.features.idea.service

import com.giwon.assistant.features.idea.dto.IdeaSummaryRequest
import com.giwon.assistant.features.idea.dto.IdeaSummaryResponse
import com.giwon.assistant.features.idea.dto.CreateIdeaRequest
import com.giwon.assistant.features.idea.dto.IdeaDetailResponse
import com.giwon.assistant.features.idea.dto.UpdateIdeaRequest
import com.giwon.assistant.features.idea.entity.IdeaEntity
import com.giwon.assistant.features.idea.model.Idea
import com.giwon.assistant.features.idea.repository.IdeaRepository
import com.giwon.assistant.common.notion.NotionIdeaExporter
import org.springframework.stereotype.Service
import java.time.OffsetDateTime
import java.util.UUID

@Service
class IdeaService(
    private val ideaSummaryProvider: IdeaSummaryProvider,
    private val ideaRepository: IdeaRepository,
    private val notionIdeaExporter: NotionIdeaExporter,
) {
    fun summarize(request: IdeaSummaryRequest): IdeaSummaryResponse =
        runCatching { ideaSummaryProvider.summarize(request) }
            .getOrElse { MockIdeaSummaryProvider().summarize(request) }

    fun create(request: CreateIdeaRequest): IdeaDetailResponse {
        val summary = summarize(IdeaSummaryRequest(title = request.title, rawText = request.rawText))
        val now = OffsetDateTime.now()
        val idea = Idea(
            id = "IDEA-${UUID.randomUUID()}",
            title = request.title,
            rawText = request.rawText,
            summary = summary.summary,
            keyPoints = summary.keyPoints,
            suggestedActions = summary.suggestedActions,
            tags = request.tags,
            status = "OPEN",
            createdAt = now,
            updatedAt = now,
        )

        val saved = ideaRepository.save(idea.toEntity()).toModel().toResponse()
        notionIdeaExporter.export(saved)
        return saved
    }

    fun getAll(): List<IdeaDetailResponse> =
        ideaRepository.findAll()
            .sortedByDescending { it.createdAt }
            .map { it.toModel().toResponse() }

    fun getById(id: String): IdeaDetailResponse =
        ideaRepository.findById(id).orElse(null)?.toModel()?.toResponse()
            ?: throw IllegalArgumentException("Idea not found: $id")

    fun update(id: String, request: UpdateIdeaRequest): IdeaDetailResponse {
        val current = ideaRepository.findById(id).orElse(null)?.toModel()
            ?: throw IllegalArgumentException("Idea not found: $id")

        val title = request.title ?: current.title
        val rawText = request.rawText ?: current.rawText
        val tags = request.tags ?: current.tags
        val status = request.status ?: current.status
        val requiresResummary = title != current.title || rawText != current.rawText

        val summary = if (requiresResummary) {
            summarize(IdeaSummaryRequest(title = title, rawText = rawText))
        } else {
            IdeaSummaryResponse(
                title = current.title,
                summary = current.summary,
                keyPoints = current.keyPoints,
                suggestedActions = current.suggestedActions,
            )
        }

        val updated = current.copy(
            title = title,
            rawText = rawText,
            summary = summary.summary,
            keyPoints = summary.keyPoints,
            suggestedActions = summary.suggestedActions,
            tags = tags,
            status = status,
            updatedAt = OffsetDateTime.now(),
        )

        return ideaRepository.save(updated.toEntity()).toModel().toResponse()
    }

    private fun Idea.toEntity(): IdeaEntity =
        IdeaEntity(
            id = id,
            title = title,
            rawText = rawText,
            summary = summary,
            keyPoints = keyPoints,
            suggestedActions = suggestedActions,
            tags = tags,
            status = status,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )

    private fun IdeaEntity.toModel(): Idea =
        Idea(
            id = id,
            title = title,
            rawText = rawText,
            summary = summary,
            keyPoints = keyPoints,
            suggestedActions = suggestedActions,
            tags = tags,
            status = status,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )

    private fun Idea.toResponse(): IdeaDetailResponse =
        IdeaDetailResponse(
            id = id,
            title = title,
            rawText = rawText,
            summary = summary,
            keyPoints = keyPoints,
            suggestedActions = suggestedActions,
            tags = tags,
            status = status,
            createdAt = createdAt.toString(),
            updatedAt = updatedAt.toString(),
        )
}
