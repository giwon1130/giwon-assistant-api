package com.giwon.assistant.common.notion

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.giwon.assistant.features.idea.repository.IdeaRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import java.time.OffsetDateTime

@Service
class NotionIdeaSyncService(
    private val notionRestClient: RestClient,
    private val properties: AssistantNotionProperties,
    private val ideaRepository: IdeaRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun syncFromNotion() {
        if (!properties.enabled || properties.ideaDatabaseId.isBlank()) return

        val ideas = ideaRepository.findAllByNotionPageIdIsNotNull()
        if (ideas.isEmpty()) return

        log.info("Syncing ${ideas.size} ideas from Notion")
        var updatedCount = 0

        ideas.forEach { idea ->
            val notionPageId = idea.notionPageId ?: return@forEach
            runCatching {
                val response = notionRestClient.get()
                    .uri("/pages/$notionPageId")
                    .retrieve()
                    .body(NotionPageResponse::class.java)
                    ?: return@runCatching

                val notionStatus = response.properties
                    ?.get("상태")
                    ?.select
                    ?.name
                    ?.uppercase()
                    ?.replace(" ", "_")

                if (notionStatus != null && notionStatus != idea.status) {
                    idea.status = notionStatus
                    idea.updatedAt = OffsetDateTime.now()
                    ideaRepository.save(idea)
                    updatedCount++
                    log.info("Idea ${idea.id} status synced: ${idea.status} → $notionStatus")
                }
            }.onFailure {
                log.warn("Failed to sync idea ${idea.id} from Notion: ${it.message}")
            }
        }

        log.info("Notion sync completed: $updatedCount/${ideas.size} updated")
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
private data class NotionPageResponse(
    val properties: Map<String, NotionProperty>? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class NotionProperty(
    val select: NotionSelectValue? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class NotionSelectValue(
    val name: String? = null,
)
