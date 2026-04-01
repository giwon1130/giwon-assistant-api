package com.giwon.assistant.features.idea.repository

import com.giwon.assistant.features.idea.model.Idea
import org.springframework.stereotype.Repository
import java.util.concurrent.ConcurrentHashMap

@Repository
class IdeaRepository {
    private val ideas = ConcurrentHashMap<String, Idea>()

    fun save(idea: Idea): Idea {
        ideas[idea.id] = idea
        return idea
    }

    fun findAll(): List<Idea> =
        ideas.values.sortedByDescending { it.createdAt }

    fun findById(id: String): Idea? =
        ideas[id]
}
