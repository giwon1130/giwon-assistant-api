package com.giwon.assistant.features.copilot.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.giwon.assistant.features.briefing.service.BriefingService
import com.giwon.assistant.features.briefing.service.CalendarProvider
import com.giwon.assistant.features.briefing.service.NewsProvider
import com.giwon.assistant.features.briefing.service.WeatherProvider
import com.giwon.assistant.features.checkin.service.DailyConditionCheckinService
import com.giwon.assistant.features.copilot.repository.CopilotHistoryRepository
import com.giwon.assistant.features.idea.service.AssistantGeminiProperties
import com.giwon.assistant.features.idea.service.AssistantOpenAiProperties
import com.giwon.assistant.features.idea.service.IdeaService
import com.giwon.assistant.features.planner.service.PlannerService
import com.giwon.assistant.features.routine.service.DailyRoutineService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.springframework.web.client.RestClient

class CopilotServiceTest {
    private val service = CopilotService(
        briefingService = mock(BriefingService::class.java),
        plannerService = mock(PlannerService::class.java),
        ideaService = mock(IdeaService::class.java),
        dailyRoutineService = mock(DailyRoutineService::class.java),
        dailyConditionCheckinService = mock(DailyConditionCheckinService::class.java),
        copilotHistoryRepository = mock(CopilotHistoryRepository::class.java),
        weatherProvider = mock(WeatherProvider::class.java),
        calendarProvider = mock(CalendarProvider::class.java),
        newsProvider = mock(NewsProvider::class.java),
        geminiRestClient = mock(RestClient::class.java),
        openAiRestClient = mock(RestClient::class.java),
        geminiProperties = AssistantGeminiProperties(model = "gemini-2.0-flash"),
        openAiProperties = AssistantOpenAiProperties(model = "gpt-4.1"),
        objectMapper = ObjectMapper(),
        geminiEnabled = true,
        openAiEnabled = true,
        geminiApiKey = "test-key",
        openAiApiKey = "test-key",
    )

    @Test
    fun `extracts output text from direct output_text field`() {
        val responseBody = """
            {
              "output_text": "ANSWER: 바로 진행해"
            }
        """.trimIndent()

        assertEquals("ANSWER: 바로 진행해", service.extractOpenAiOutputText(responseBody))
    }

    @Test
    fun `extracts output text from output content text field`() {
        val responseBody = """
            {
              "output": [
                {
                  "type": "message",
                  "content": [
                    {
                      "type": "output_text",
                      "text": "ANSWER: 오전에는 핵심 작업부터 처리해\nREASONING:\n- 근거 1\nSUGGESTED_ACTIONS:\n- 액션 1"
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        assertEquals(
            "ANSWER: 오전에는 핵심 작업부터 처리해\nREASONING:\n- 근거 1\nSUGGESTED_ACTIONS:\n- 액션 1",
            service.extractOpenAiOutputText(responseBody)
        )
    }

    @Test
    fun `extracts output text from gemini candidates payload`() {
        val responseBody = """
            {
              "candidates": [
                {
                  "content": {
                    "parts": [
                      {
                        "text": "ANSWER: Gemini 응답\nREASONING:\n- 근거 1\nSUGGESTED_ACTIONS:\n- 액션 1"
                      }
                    ]
                  }
                }
              ]
            }
        """.trimIndent()

        assertEquals(
            "ANSWER: Gemini 응답\nREASONING:\n- 근거 1\nSUGGESTED_ACTIONS:\n- 액션 1",
            service.extractGeminiOutputText(responseBody)
        )
    }
}
