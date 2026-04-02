package com.giwon.assistant.features.briefing.service

import com.giwon.assistant.features.briefing.dto.HeadlineItem

fun interface NewsProvider {
    fun getTopHeadlines(): List<HeadlineItem>
}
