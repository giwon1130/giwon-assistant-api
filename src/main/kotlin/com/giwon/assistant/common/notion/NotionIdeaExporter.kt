package com.giwon.assistant.common.notion

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.giwon.assistant.features.idea.dto.IdeaDetailResponse
import com.giwon.assistant.features.idea.repository.IdeaRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.time.OffsetDateTime

@Component
class NotionIdeaExporter(
    private val notionRestClient: RestClient,
    private val properties: AssistantNotionProperties,
    private val ideaRepository: IdeaRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun export(idea: IdeaDetailResponse) {
        if (!properties.enabled || properties.ideaDatabaseId.isBlank()) return

        val date = runCatching { idea.createdAt.substring(0, 10) }.getOrElse { idea.createdAt }

        val body = mapOf(
            "parent" to mapOf("database_id" to properties.ideaDatabaseId),
            "properties" to mapOf(
                "제목" to mapOf("title" to listOf(mapOf("text" to mapOf("content" to idea.title)))),
                "요약" to mapOf("rich_text" to listOf(mapOf("text" to mapOf("content" to idea.summary)))),
                "태그" to mapOf("multi_select" to idea.tags.map { mapOf("name" to it) }),
                "상태" to mapOf("select" to mapOf("name" to idea.status)),
                "생성일" to mapOf("date" to mapOf("start" to date)),
            ),
        )

        runCatching {
            val response = notionRestClient.post()
                .uri("/pages")
                .body(body)
                .retrieve()
                .body(NotionPageCreatedResponse::class.java)

            val notionPageId = response?.id
            if (notionPageId != null) {
                ideaRepository.findById(idea.id).ifPresent { entity ->
                    entity.notionPageId = notionPageId
                    ideaRepository.save(entity)
                }
                log.info("Idea exported to Notion: ${idea.title} (pageId=$notionPageId)")
            }
        }.onFailure {
            log.warn("Failed to export idea to Notion: ${it.message}")
        }
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
private data class NotionPageCreatedResponse(val id: String? = null)
