package com.giwon.assistant.features.routine.repository

import com.giwon.assistant.features.routine.entity.DailyRoutineCheckEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

interface DailyRoutineCheckRepository : JpaRepository<DailyRoutineCheckEntity, String> {
    fun findAllByCheckDate(checkDate: LocalDate): List<DailyRoutineCheckEntity>

    fun findAllByCheckDateBetween(startDate: LocalDate, endDate: LocalDate): List<DailyRoutineCheckEntity>
}
