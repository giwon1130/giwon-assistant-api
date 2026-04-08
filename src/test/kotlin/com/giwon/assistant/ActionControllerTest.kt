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
class ActionControllerTest {
    @Autowired
    lateinit var mockMvc: MockMvc

    @Test
    fun `action tracker create and update flow works`() {
        val createResult = mockMvc.perform(
            post("/api/v1/actions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "title": "AI 비서 API MVP 확정",
                      "sourceQuestion": "오늘 뭐부터 하면 좋을까?",
                      "priority": "HIGH",
                      "dueDate": "2026-04-10T09:00:00+09:00"
                    }
                    """.trimIndent()
                )
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.status").value("OPEN"))
            .andExpect(jsonPath("$.data.priority").value("HIGH"))
            .andReturn()

        val actionId = com.jayway.jsonpath.JsonPath.read<String>(
            createResult.response.contentAsString,
            "$.data.id"
        )

        mockMvc.perform(get("/api/v1/actions?status=OPEN"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data[0].id").exists())

        mockMvc.perform(
            patch("/api/v1/actions/$actionId")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"priority":"LOW","dueDate":"2026-04-11T18:00:00+09:00"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.priority").value("LOW"))
            .andExpect(jsonPath("$.data.dueDate").exists())

        mockMvc.perform(
            patch("/api/v1/actions/$actionId/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"status":"DONE"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.status").value("DONE"))
            .andExpect(jsonPath("$.data.completedAt").exists())
    }
}
