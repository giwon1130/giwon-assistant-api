package com.giwon.assistant.features.briefing.service

import com.giwon.assistant.features.briefing.dto.TaskItem

fun interface TaskProvider {
    fun getOpenTasks(): List<TaskItem>
}
