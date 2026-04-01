package com.giwon.assistant.features.idea.service

import com.giwon.assistant.features.idea.dto.IdeaSummaryRequest
import com.giwon.assistant.features.idea.dto.IdeaSummaryResponse
import org.springframework.stereotype.Service

@Service
class IdeaService(
    private val ideaSummaryProvider: IdeaSummaryProvider,
) {
    fun summarize(request: IdeaSummaryRequest): IdeaSummaryResponse =
        runCatching { ideaSummaryProvider.summarize(request) }
            .getOrElse { MockIdeaSummaryProvider().summarize(request) }
}
