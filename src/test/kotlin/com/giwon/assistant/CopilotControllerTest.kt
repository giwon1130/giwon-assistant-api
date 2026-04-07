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
class CopilotControllerTest {
    @Autowired
    lateinit var mockMvc: MockMvc

    @Test
    fun `today copilot endpoint returns integrated assistant view`() {
        mockMvc.perform(
            post("/api/v1/ideas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "title": "AI 비서 코파일럿",
                      "rawText": "브리핑과 일정, 아이디어를 한 번에 묶어 오늘 해야 할 일을 추천해주는 기능을 만들고 싶다.",
                      "tags": ["assistant", "copilot"]
                    }
                    """.trimIndent()
                )
        ).andExpect(status().isOk)

        mockMvc.perform(get("/api/v1/copilot/today"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.headline").exists())
            .andExpect(jsonPath("$.data.topPriority").exists())
            .andExpect(jsonPath("$.data.recommendedIdeas[0].title").exists())
            .andExpect(jsonPath("$.data.todayFlow.length()").value(3))
    }
}
