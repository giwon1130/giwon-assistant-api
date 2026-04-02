package com.giwon.assistant.features.briefing.service

import com.giwon.assistant.features.briefing.dto.HeadlineItem
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.w3c.dom.Element
import java.io.ByteArrayInputStream
import javax.xml.parsers.DocumentBuilderFactory

@Component
@ConditionalOnProperty(
    prefix = "assistant.integrations",
    name = ["news-enabled"],
    havingValue = "true",
    matchIfMissing = true,
)
class GoogleNewsRssProvider(
    private val newsRestClient: RestClient,
    private val newsProperties: AssistantNewsProperties,
) : NewsProvider {
    override fun getTopHeadlines(): List<HeadlineItem> {
        val rssBody = newsRestClient.get()
            .uri(newsProperties.rssUrl)
            .retrieve()
            .body(String::class.java)
            ?: error("News RSS response is empty")

        val documentBuilderFactory = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = false
            isExpandEntityReferences = false
        }
        val builder = documentBuilderFactory.newDocumentBuilder()
        val document = builder.parse(ByteArrayInputStream(rssBody.toByteArray()))
        val items = document.getElementsByTagName("item")

        return (0 until minOf(items.length, newsProperties.limit))
            .mapNotNull { index ->
                val item = items.item(index) as? Element ?: return@mapNotNull null
                val titleText = item.getElementsByTagName("title").item(0)?.textContent?.trim().orEmpty()
                if (titleText.isBlank()) return@mapNotNull null

                val explicitSource = item.getElementsByTagName("source").item(0)?.textContent?.trim()
                val (headline, fallbackSource) = splitTitleAndSource(titleText)

                HeadlineItem(
                    source = explicitSource?.takeIf { it.isNotBlank() } ?: fallbackSource,
                    title = headline,
                )
            }
    }

    private fun splitTitleAndSource(rawTitle: String): Pair<String, String> {
        val separatorIndex = rawTitle.lastIndexOf(" - ")
        if (separatorIndex == -1) {
            return rawTitle to "News"
        }

        val headline = rawTitle.substring(0, separatorIndex).trim()
        val source = rawTitle.substring(separatorIndex + 3).trim().ifBlank { "News" }
        return headline to source
    }
}
