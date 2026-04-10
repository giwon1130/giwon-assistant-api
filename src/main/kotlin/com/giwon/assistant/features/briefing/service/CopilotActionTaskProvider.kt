package com.giwon.assistant.features.briefing.service

import com.giwon.assistant.features.action.repository.CopilotActionRepository
import com.giwon.assistant.features.briefing.dto.TaskItem
import org.springframework.stereotype.Component

@Component
class CopilotActionTaskProvider(
    private val copilotActionRepository: CopilotActionRepository,
) : TaskProvider {
    companion object {
        private val PRIORITY_ORDER = mapOf("HIGH" to 0, "MEDIUM" to 1, "LOW" to 2)
        private const val MAX_TASKS = 5
    }

    override fun getOpenTasks(): List<TaskItem> =
        copilotActionRepository.findAllByOrderByCreatedAtDesc()
            .filter { it.status == "OPEN" }
            .sortedBy { PRIORITY_ORDER[it.priority] ?: 3 }
            .take(MAX_TASKS)
            .map { TaskItem(priority = it.priority, title = it.title) }
            .ifEmpty { mockTasks() }

    private fun mockTasks(): List<TaskItem> =
        listOf(
            TaskItem(priority = "HIGH", title = "AI 비서 MVP API 구조 확정", mock = true),
            TaskItem(priority = "MEDIUM", title = "giwon-home에 live 링크 연결", mock = true),
            TaskItem(priority = "LOW", title = "아이디어 노트 정리", mock = true),
        )
}
