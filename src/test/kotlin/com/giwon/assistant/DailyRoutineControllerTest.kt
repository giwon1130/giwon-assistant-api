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
class DailyRoutineControllerTest {
    @Autowired
    lateinit var mockMvc: MockMvc

    @Test
    fun `daily routine get and update flow works`() {
        mockMvc.perform(get("/api/v1/routines/daily"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.totalCount").value(6))
            .andExpect(jsonPath("$.data.items[0].key").exists())

        mockMvc.perform(
            patch("/api/v1/routines/daily/VITAMIN")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"completed":true,"note":"아침 식후 복용"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.completedCount").value(1))
            .andExpect(jsonPath("$.data.items[0].completed").exists())

        mockMvc.perform(get("/api/v1/routines/daily"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.completedCount").value(1))
            .andExpect(jsonPath("$.data.items[?(@.key=='VITAMIN')].note").exists())
    }
}
