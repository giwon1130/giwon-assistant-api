package com.giwon.assistant.features.copilot.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.giwon.assistant.features.briefing.service.AssistantAnthropicProperties
import com.giwon.assistant.features.idea.service.AssistantGeminiProperties
import com.giwon.assistant.features.idea.service.AssistantOpenAiProperties
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.springframework.web.client.RestClient

class CopilotServiceTest {
    private val llmService = CopilotLlmService(
        geminiRestClient = mock(RestClient::class.java),
        claudeRestClient = mock(RestClient::class.java),
        openAiRestClient = mock(RestClient::class.java),
        geminiProperties = AssistantGeminiProperties(model = "gemini-2.0-flash"),
        openAiProperties = AssistantOpenAiProperties(model = "gpt-4.1"),
        anthropicProperties = AssistantAnthropicProperties(model = "claude-sonnet-4-5"),
        objectMapper = ObjectMapper(),
        geminiEnabled = false,
        openAiEnabled = false,
        claudeEnabled = false,
        geminiApiKey = "",
        openAiApiKey = "",
        anthropicApiKey = "",
    )

    @Test
    fun `extracts output text from direct output_text field`() {
        val responseBody = """
            {
              "output_text": "ANSWER: 바로 진행해"
            }
        """.trimIndent()

        assertEquals("ANSWER: 바로 진행해", llmService.extractOpenAiOutputText(responseBody))
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
            llmService.extractOpenAiOutputText(responseBody)
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
            llmService.extractGeminiOutputText(responseBody)
        )
    }
}
