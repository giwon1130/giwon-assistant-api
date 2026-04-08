package com.giwon.assistant.features.action.service

import com.giwon.assistant.features.action.dto.CopilotActionResponse
import com.giwon.assistant.features.action.dto.CopilotActionSummaryResponse
import com.giwon.assistant.features.action.dto.CreateCopilotActionRequest
import com.giwon.assistant.features.action.dto.UpdateCopilotActionRequest
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
            priority = normalizePriority(request.priority),
            dueDate = request.dueDate?.let(OffsetDateTime::parse),
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

    fun getSummary(): CopilotActionSummaryResponse {
        val now = OffsetDateTime.now()
        val actions = copilotActionRepository.findAll()
        val openActions = actions.filter { it.status == "OPEN" }
        val doneCount = actions.count { it.status == "DONE" }
        val overdueCount = openActions.count { it.dueDate != null && it.dueDate!!.isBefore(now) }
        val dueSoonCount = openActions.count {
            val dueDate = it.dueDate ?: return@count false
            !dueDate.isBefore(now) && dueDate.isBefore(now.plusHours(24))
        }
        val highPriorityOpenCount = openActions.count { it.priority == "HIGH" }
        val completionRate = if (actions.isEmpty()) 0 else ((doneCount.toDouble() / actions.size) * 100).toInt()

        return CopilotActionSummaryResponse(
            totalCount = actions.size,
            openCount = openActions.size,
            doneCount = doneCount,
            overdueCount = overdueCount,
            dueSoonCount = dueSoonCount,
            highPriorityOpenCount = highPriorityOpenCount,
            completionRate = completionRate,
        )
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

    fun update(actionId: String, request: UpdateCopilotActionRequest): CopilotActionResponse {
        val current = copilotActionRepository.findById(actionId).orElse(null)
            ?: throw IllegalArgumentException("Action not found: $actionId")

        current.title = request.title?.trim()?.ifBlank { current.title } ?: current.title
        current.priority = request.priority?.let(::normalizePriority) ?: current.priority
        current.dueDate = request.dueDate?.let(OffsetDateTime::parse)
        current.updatedAt = OffsetDateTime.now()
        return copilotActionRepository.save(current).toResponse()
    }

    private fun normalizePriority(priority: String?): String {
        val normalized = priority?.trim()?.uppercase() ?: "MEDIUM"
        require(normalized == "LOW" || normalized == "MEDIUM" || normalized == "HIGH") {
            "priority must be LOW, MEDIUM or HIGH"
        }
        return normalized
    }

    private fun CopilotActionEntity.toResponse(): CopilotActionResponse =
        CopilotActionResponse(
            id = id,
            title = title,
            sourceQuestion = sourceQuestion,
            status = status,
            priority = priority,
            dueDate = dueDate?.toString(),
            createdAt = createdAt.toString(),
            updatedAt = updatedAt.toString(),
            completedAt = completedAt?.toString(),
        )
}
