package com.giwon.assistant.features.briefing.service

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(
    prefix = "assistant.integrations",
    name = ["claude-enabled"],
    havingValue = "false",
    matchIfMissing = true,
)
class MockBriefingSummaryProvider : BriefingSummaryProvider {
    override fun summarize(request: BriefingSummaryRequest): BriefingSummaryResult =
        BriefingSummaryResult(
            summary = "오늘은 오전 집중 작업과 오후 미팅이 있어. 중요한 작업을 먼저 끝내는 흐름이 좋다. [목데이터]",
            focusSuggestion = "오전에는 설계와 구현을 한 번에 끝내고, 오후에는 연결 작업과 정리에 집중하는 게 좋다. [목데이터]",
        )
}
