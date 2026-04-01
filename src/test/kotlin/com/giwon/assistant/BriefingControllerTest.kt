package com.giwon.assistant

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
class BriefingControllerTest {
    @Autowired
    lateinit var mockMvc: MockMvc

    @Test
    fun `today briefing endpoint returns assistant summary`() {
        mockMvc.perform(get("/api/v1/briefings/today"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.weather.location").value("Seoul"))
            .andExpect(jsonPath("$.data.tasks.length()").value(3))
    }
}
