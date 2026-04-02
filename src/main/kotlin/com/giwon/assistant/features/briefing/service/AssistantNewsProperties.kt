package com.giwon.assistant.features.briefing.service

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "assistant.news")
data class AssistantNewsProperties(
    val rssUrl: String = "https://news.google.com/rss?hl=ko&gl=KR&ceid=KR:ko",
    val limit: Int = 3,
)
