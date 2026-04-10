package com.giwon.assistant.common.notion

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient

@Configuration
@EnableConfigurationProperties(AssistantNotionProperties::class)
class NotionClientConfig {
    @Bean
    fun notionRestClient(properties: AssistantNotionProperties): RestClient =
        RestClient.builder()
            .baseUrl("https://api.notion.com/v1")
            .defaultHeader("Authorization", "Bearer ${properties.token}")
            .defaultHeader("Notion-Version", "2022-06-28")
            .defaultHeader("Content-Type", "application/json")
            .build()
}
