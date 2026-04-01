package com.giwon.assistant.features.idea.repository

import com.giwon.assistant.features.idea.entity.IdeaEntity
import org.springframework.data.jpa.repository.JpaRepository

interface IdeaRepository : JpaRepository<IdeaEntity, String>
