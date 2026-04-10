package com.giwon.assistant.features.briefing.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient

@Service
class BriefingAudioService(
    private val briefingService: BriefingService,
    private val weatherProvider: WeatherProvider,
    private val calendarProvider: CalendarProvider,
    private val newsProvider: NewsProvider,
    @Qualifier("openAiRestClient") private val openAiRestClient: RestClient,
    @Value("\${assistant.integrations.openai-enabled:false}") private val openAiEnabled: Boolean,
    @Value("\${OPENAI_API_KEY:}") private val openAiApiKey: String,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * 오늘 브리핑 요약을 TTS로 변환해 mp3 바이트를 반환한다.
     * OpenAI가 설정되지 않으면 null 반환.
     */
    fun generateAudio(): ByteArray? {
        if (!openAiEnabled || openAiApiKey.isBlank()) {
            log.warn("OpenAI not enabled — audio briefing unavailable")
            return null
        }

        val briefing = briefingService.getTodayBriefing(weatherProvider, calendarProvider, newsProvider)
        val script = buildScript(briefing)

        return runCatching {
            openAiRestClient.post()
                .uri("/v1/audio/speech")
                .header("Authorization", "Bearer $openAiApiKey")
                .header("Content-Type", "application/json")
                .body(
                    mapOf(
                        "model" to "tts-1",
                        "input" to script,
                        "voice" to "nova",
                        "response_format" to "mp3",
                    )
                )
                .retrieve()
                .body(ByteArray::class.java)
        }.getOrElse { e ->
            log.error("TTS generation failed: ${e.message}")
            null
        }
    }

    private fun buildScript(briefing: com.giwon.assistant.features.briefing.dto.TodayBriefingResponse): String {
        val calendarText = briefing.calendar.take(3)
            .joinToString(", ") { "${it.time}에 ${it.title}" }
            .ifBlank { "오늘 일정 없음" }

        val tasksText = briefing.tasks.take(3)
            .joinToString(", ") { it.title }
            .ifBlank { "할 일 없음" }

        return """
            안녕하세요, 오늘의 브리핑입니다.
            ${briefing.summary}
            오늘 날씨는 ${briefing.weather.condition}, ${briefing.weather.temperatureCelsius}도입니다.
            오늘 일정은 $calendarText 입니다.
            오늘 할 일은 $tasksText 입니다.
            집중 제안: ${briefing.focusSuggestion}
            좋은 하루 되세요.
        """.trimIndent()
    }
}
