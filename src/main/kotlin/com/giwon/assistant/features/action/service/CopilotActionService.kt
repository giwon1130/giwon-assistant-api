package com.giwon.assistant.features.action.service

import com.giwon.assistant.features.action.dto.CopilotActionResponse
import com.giwon.assistant.features.action.dto.CreateCopilotActionRequest
import com.giwon.assistant.features.action.entity.CopilotActionEntity
import com.giwon.assistant.features.action.repository.CopilotActionRepository
import org.springframework.stereotype.Service
import java.time.OffsetDateTime
import java.util.UUID

@Service
class CopilotActionService(
    private val copilotActionRepository: CopilotActionRepository,
) {
    fun create(request: CreateCopilotActionRequest): CopilotActionResponse {
        val now = OffsetDateTime.now()
        val entity = CopilotActionEntity(
            id = "ACTION-${UUID.randomUUID()}",
            title = request.title.trim(),
            sourceQuestion = request.sourceQuestion.trim(),
            status = "OPEN",
            createdAt = now,
            updatedAt = now,
            completedAt = null,
        )
        return copilotActionRepository.save(entity).toResponse()
    }

    fun getAll(status: String?): List<CopilotActionResponse> {
        val normalized = status?.trim()?.uppercase()
        val actions = copilotActionRepository.findAllByOrderByCreatedAtDesc()
        return actions
            .filter { normalized == null || it.status == normalized }
            .map { it.toResponse() }
    }

    fun updateStatus(actionId: String, status: String): CopilotActionResponse {
        val normalized = status.trim().uppercase()
        require(normalized == "OPEN" || normalized == "DONE") { "status must be OPEN or DONE" }

        val current = copilotActionRepository.findById(actionId).orElse(null)
            ?: throw IllegalArgumentException("Action not found: $actionId")

        val now = OffsetDateTime.now()
        current.status = normalized
        current.updatedAt = now
        current.completedAt = if (normalized == "DONE") now else null
        return copilotActionRepository.save(current).toResponse()
    }

    private fun CopilotActionEntity.toResponse(): CopilotActionResponse =
        CopilotActionResponse(
            id = id,
            title = title,
            sourceQuestion = sourceQuestion,
            status = status,
            createdAt = createdAt.toString(),
            updatedAt = updatedAt.toString(),
            completedAt = completedAt?.toString(),
        )
}
