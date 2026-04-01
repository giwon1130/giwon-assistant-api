package com.giwon.assistant.features.idea.service

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient

@Configuration
@EnableConfigurationProperties(AssistantOpenAiProperties::class)
class OpenAiClientConfig {
    @Bean
    fun openAiRestClient(): RestClient =
        RestClient.builder()
            .baseUrl("https://api.openai.com")
            .build()
}
