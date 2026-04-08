package com.giwon.assistant.features.routine.service

import com.giwon.assistant.features.routine.dto.DailyRoutineItemResponse
import com.giwon.assistant.features.routine.dto.DailyRoutineResponse
import com.giwon.assistant.features.routine.dto.UpdateDailyRoutineRequest
import com.giwon.assistant.features.routine.entity.DailyRoutineCheckEntity
import com.giwon.assistant.features.routine.repository.DailyRoutineCheckRepository
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset

@Service
class DailyRoutineService(
    private val dailyRoutineCheckRepository: DailyRoutineCheckRepository,
) {
    fun getDailyRoutine(date: String?): DailyRoutineResponse {
        val targetDate = date?.let(LocalDate::parse) ?: LocalDate.now(ZoneOffset.UTC)
        return buildResponse(targetDate)
    }

    fun updateDailyRoutine(date: String?, itemKey: String, request: UpdateDailyRoutineRequest): DailyRoutineResponse {
        val targetDate = date?.let(LocalDate::parse) ?: LocalDate.now(ZoneOffset.UTC)
        val spec = routineSpecs.find { it.key == itemKey.trim().uppercase() }
            ?: throw IllegalArgumentException("Unknown routine item: $itemKey")

        val normalizedItemKey = spec.key
        val id = "${targetDate}-$normalizedItemKey"
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val current = dailyRoutineCheckRepository.findById(id).orElse(
            DailyRoutineCheckEntity(
                id = id,
                checkDate = targetDate,
                itemKey = normalizedItemKey,
                completed = false,
                note = null,
                completedAt = null,
                updatedAt = now,
            )
        )

        current.completed = request.completed
        current.note = request.note?.trim()?.ifBlank { null }
        current.completedAt = if (request.completed) now else null
        current.updatedAt = now
        dailyRoutineCheckRepository.save(current)
        return buildResponse(targetDate)
    }

    fun getCompletedChecksBetween(startDate: LocalDate, endDateExclusive: LocalDate): Int =
        dailyRoutineCheckRepository.findAllByCheckDateBetween(startDate, endDateExclusive.minusDays(1))
            .count { it.completed }

    fun getCompletedDaysBetween(startDate: LocalDate, endDateExclusive: LocalDate): Int =
        dailyRoutineCheckRepository.findAllByCheckDateBetween(startDate, endDateExclusive.minusDays(1))
            .filter { it.completed }
            .groupBy { it.checkDate }
            .count { (_, items) -> items.size >= 3 }

    private fun buildResponse(targetDate: LocalDate): DailyRoutineResponse {
        val checksByKey = dailyRoutineCheckRepository.findAllByCheckDate(targetDate)
            .associateBy { it.itemKey }
        val items = routineSpecs.map { spec ->
            val saved = checksByKey[spec.key]
            DailyRoutineItemResponse(
                key = spec.key,
                label = spec.label,
                description = spec.description,
                category = spec.category,
                targetTime = spec.targetTime,
                completed = saved?.completed ?: false,
                completedAt = saved?.completedAt?.toString(),
                note = saved?.note,
            )
        }
        val completedCount = items.count { it.completed }
        val completionRate = if (items.isEmpty()) 0 else ((completedCount.toDouble() / items.size) * 100).toInt()

        return DailyRoutineResponse(
            date = targetDate.toString(),
            completionRate = completionRate,
            completedCount = completedCount,
            totalCount = items.size,
            streakDays = calculateStreak(targetDate),
            items = items,
        )
    }

    private fun calculateStreak(targetDate: LocalDate): Int {
        val checks = dailyRoutineCheckRepository.findAllByCheckDateBetween(targetDate.minusDays(13), targetDate)
            .filter { it.completed }
            .groupBy { it.checkDate }
            .mapValues { (_, items) -> items.size }

        var streak = 0
        var cursor = targetDate
        while (checks[cursor]?.let { it >= 3 } == true) {
            streak += 1
            cursor = cursor.minusDays(1)
        }
        return streak
    }

    private data class RoutineSpec(
        val key: String,
        val label: String,
        val description: String,
        val category: String,
        val targetTime: String,
    )

    companion object {
        private val routineSpecs = listOf(
            RoutineSpec("VITAMIN", "비타민", "아침에 영양제나 비타민을 챙겨 먹었는지 체크", "HEALTH", "아침"),
            RoutineSpec("WATER", "물 2L", "수분 섭취 목표를 채웠는지 체크", "HEALTH", "하루"),
            RoutineSpec("WORKOUT", "운동", "가벼운 운동이나 스트레칭을 했는지 체크", "ENERGY", "저녁"),
            RoutineSpec("WALK", "산책", "짧게라도 걷기 루틴을 지켰는지 체크", "ENERGY", "오후"),
            RoutineSpec("MEDICATION", "약 복용", "필요한 약을 놓치지 않았는지 체크", "HEALTH", "아침/저녁"),
            RoutineSpec("SLEEP", "수면 준비", "자기 전 수면 루틴을 챙겼는지 체크", "RECOVERY", "밤"),
        )
    }
}
