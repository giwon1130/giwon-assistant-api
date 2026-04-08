package com.giwon.assistant.features.checkin.repository

import com.giwon.assistant.features.checkin.entity.DailyConditionCheckinEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

interface DailyConditionCheckinRepository : JpaRepository<DailyConditionCheckinEntity, LocalDate> {
    fun findAllByCheckDateBetween(startDate: LocalDate, endDate: LocalDate): List<DailyConditionCheckinEntity>
}
