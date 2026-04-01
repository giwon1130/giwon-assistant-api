package com.giwon.assistant.features.idea.service

import com.giwon.assistant.features.idea.dto.IdeaSummaryRequest
import com.giwon.assistant.features.idea.dto.IdeaSummaryResponse
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(
    prefix = "assistant.integrations",
    name = ["openai-enabled"],
    havingValue = "false",
    matchIfMissing = true,
)
class MockIdeaSummaryProvider : IdeaSummaryProvider {
    override fun summarize(request: IdeaSummaryRequest): IdeaSummaryResponse {
        val normalizedTitle = request.title?.takeIf { it.isNotBlank() } ?: "Untitled Idea"
        val compactText = request.rawText
            .replace("\n", " ")
            .trim()
            .replace(Regex("\\s+"), " ")

        val summary = compactText.take(120).let {
            if (compactText.length > 120) "$it..." else it
        }

        return IdeaSummaryResponse(
            title = normalizedTitle,
            summary = summary,
            keyPoints = listOf(
                "아이디어의 목적을 한 문장으로 다시 정리할 필요가 있음",
                "필수 기능과 나중에 붙일 기능을 분리하면 구현 속도가 빨라짐",
                "사용 시점과 자동화 조건을 명확히 정의하는 게 중요함",
            ),
            suggestedActions = listOf(
                "MVP 범위를 3개 기능 이하로 축소",
                "외부 연동 우선순위 정리",
                "실행 흐름을 아침 브리핑/아이디어 정리/캘린더 보조로 나누기",
            ),
        )
    }
}
