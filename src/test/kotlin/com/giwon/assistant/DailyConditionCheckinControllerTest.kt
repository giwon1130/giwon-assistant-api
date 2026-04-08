package com.giwon.assistant

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
class DailyConditionCheckinControllerTest {
    @Autowired
    lateinit var mockMvc: MockMvc

    @Test
    fun `condition checkin get and update works`() {
        mockMvc.perform(get("/api/v1/checkins/condition"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.energy").value(3))
            .andExpect(jsonPath("$.data.readinessScore").exists())
            .andExpect(jsonPath("$.data.summary").exists())
            .andExpect(jsonPath("$.data.suggestions").isArray)
            .andExpect(jsonPath("$.data.recentReadiness.length()").value(7))

        mockMvc.perform(
            patch("/api/v1/checkins/condition")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "energy": 4,
                      "focus": 5,
                      "mood": 4,
                      "stress": 2,
                      "sleepQuality": 4,
                      "note": "오전 컨디션 좋음"
                    }
                    """.trimIndent()
                )
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.energy").value(4))
            .andExpect(jsonPath("$.data.focus").value(5))
            .andExpect(jsonPath("$.data.note").value("오전 컨디션 좋음"))
    }
}
