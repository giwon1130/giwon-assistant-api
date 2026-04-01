package com.giwon.assistant.features.idea.service

import com.giwon.assistant.features.idea.dto.IdeaSummaryRequest
import com.giwon.assistant.features.idea.dto.IdeaSummaryResponse

fun interface IdeaSummaryProvider {
    fun summarize(request: IdeaSummaryRequest): IdeaSummaryResponse
}
