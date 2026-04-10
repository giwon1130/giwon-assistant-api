package com.giwon.assistant.features.briefing.service

import com.giwon.assistant.features.briefing.dto.HeadlineItem
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(
    prefix = "assistant.integrations",
    name = ["news-enabled"],
    havingValue = "false",
)
class MockNewsProvider : NewsProvider {
    override fun getTopHeadlines(): List<HeadlineItem> =
        listOf(
            HeadlineItem(source = "Tech", title = "AI 제품화 경쟁이 심화되는 중", mock = true),
            HeadlineItem(source = "Local", title = "날씨 변동 폭이 커 외출 전 확인 필요", mock = true),
            HeadlineItem(source = "Product", title = "개인 생산성 도구 시장이 빠르게 확장되는 중", mock = true),
        )
}
