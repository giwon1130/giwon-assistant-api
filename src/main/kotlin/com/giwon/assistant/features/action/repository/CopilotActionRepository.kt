package com.giwon.assistant.features.action.repository

import com.giwon.assistant.features.action.entity.CopilotActionEntity
import org.springframework.data.jpa.repository.JpaRepository

interface CopilotActionRepository : JpaRepository<CopilotActionEntity, String> {
    fun findAllByOrderByCreatedAtDesc(): List<CopilotActionEntity>
}
