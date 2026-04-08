package com.giwon.assistant

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
class WeeklyReviewControllerTest {
    @Autowired
    lateinit var mockMvc: MockMvc

    @Test
    fun `weekly review endpoint returns weekly summary`() {
        mockMvc.perform(
            post("/api/v1/ideas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "title": "주간 회고 테스트",
                      "rawText": "이번 주 액션과 질문 로그를 바탕으로 회고를 만들고 싶다.",
                      "tags": ["review"]
                    }
                    """.trimIndent()
                )
        ).andExpect(status().isOk)

        mockMvc.perform(
            post("/api/v1/actions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "title": "주간 회고 액션",
                      "sourceQuestion": "이번 주 무엇을 회고해야 할까?"
                    }
                    """.trimIndent()
                )
        ).andExpect(status().isOk)

        mockMvc.perform(get("/api/v1/reviews/weekly"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.summary").exists())
            .andExpect(jsonPath("$.data.metrics.questionsAsked").exists())
            .andExpect(jsonPath("$.data.wins[0]").exists())
            .andExpect(jsonPath("$.data.nextFocus[0]").exists())

        mockMvc.perform(get("/api/v1/reviews/weekly"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))

        mockMvc.perform(get("/api/v1/reviews/weekly/history"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].summary").exists())
            .andExpect(jsonPath("$.data[0].generatedAt").exists())
    }
}
