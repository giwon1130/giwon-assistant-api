package com.giwon.assistant

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
class IdeaControllerTest {
    @Autowired
    lateinit var mockMvc: MockMvc

    @Test
    fun `create and update idea`() {
        val createResponse = mockMvc.perform(
            post("/api/v1/ideas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "title": "AI 비서 자동화",
                      "rawText": "아침마다 날씨와 일정, 뉴스, 할 일을 정리해주는 기능을 만들고 싶다.",
                      "tags": ["assistant", "automation"]
                    }
                    """.trimIndent()
                )
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.title").value("AI 비서 자동화"))
            .andReturn()

        val id = "\"id\":\"([^\"]+)\"".toRegex()
            .find(createResponse.response.contentAsString)
            ?.groupValues
            ?.get(1)
            ?: error("idea id not found")

        mockMvc.perform(get("/api/v1/ideas"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data[0].title").value("AI 비서 자동화"))

        mockMvc.perform(
            patch("/api/v1/ideas/$id")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"status":"IN_PROGRESS"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"))
    }
}
