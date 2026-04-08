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

    @Test
    fun `copilot ask endpoint returns actionable answer`() {
        mockMvc.perform(
            post("/api/v1/copilot/ask")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"question":"오늘 뭐부터 하면 좋을까?"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.answer").exists())
            .andExpect(jsonPath("$.data.reasoning[0]").exists())
            .andExpect(jsonPath("$.data.suggestedActions[0]").exists())
            .andExpect(jsonPath("$.data.suggestedActionPlans[0].title").exists())
            .andExpect(jsonPath("$.data.suggestedActionPlans[0].priority").exists())
            .andExpect(jsonPath("$.data.suggestedActionPlans[0].dueLabel").exists())
    }

    @Test
    fun `copilot history endpoint returns saved answers`() {
        mockMvc.perform(
            post("/api/v1/copilot/ask")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"question":"오늘 일정 기준으로 언제 집중 작업하는 게 좋을까?"}""")
        ).andExpect(status().isOk)

        mockMvc.perform(get("/api/v1/copilot/history"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data[0].question").exists())
            .andExpect(jsonPath("$.data[0].intent").exists())
            .andExpect(jsonPath("$.data[0].answer").exists())
    }
}
