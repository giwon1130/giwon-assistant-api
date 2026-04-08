package com.giwon.assistant.features.routine.service

import com.giwon.assistant.features.routine.dto.DailyRoutineCategoryStatResponse
import com.giwon.assistant.features.routine.dto.DailyRoutineDaySummaryResponse
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
        val weekStart = targetDate.minusDays(6)
        val checksByDate = dailyRoutineCheckRepository.findAllByCheckDateBetween(weekStart, targetDate)
            .groupBy { it.checkDate }
        val recentDays = (0..6).map { offset ->
            val date = weekStart.plusDays(offset.toLong())
            val dayChecks = checksByDate[date].orEmpty()
            val dayCompletedCount = dayChecks.count { it.completed }
            DailyRoutineDaySummaryResponse(
                date = date.toString(),
                completedCount = dayCompletedCount,
                totalCount = routineSpecs.size,
                completionRate = ((dayCompletedCount.toDouble() / routineSpecs.size) * 100).toInt(),
            )
        }
        val weeklyCompletedDays = recentDays.count { it.completedCount >= 3 }
        val weeklyCompletionRate = if (recentDays.isEmpty()) 0 else (recentDays.sumOf { it.completionRate } / recentDays.size)
        val categoryStats = routineSpecs.groupBy { it.category }
            .map { (category, specs) ->
                val completedInCategory = items.count { it.category == category && it.completed }
                DailyRoutineCategoryStatResponse(
                    category = category,
                    completedCount = completedInCategory,
                    totalCount = specs.size,
                )
            }
        val incompleteItems = items.filter { !it.completed }
        val insight = when {
            completedCount == items.size ->
                "오늘 루틴을 전부 체크했어. 지금은 이 흐름을 내일까지 유지하는 게 핵심이야."
            incompleteItems.any { it.category == "HEALTH" } ->
                "건강 루틴이 아직 남아 있어. 비타민, 물, 약 복용 같은 기본 항목부터 먼저 닫는 게 좋아."
            incompleteItems.any { it.category == "RECOVERY" } ->
                "회복 루틴이 비어 있어. 밤 루틴과 수면 준비를 오늘 마감 루틴으로 고정하는 편이 좋다."
            else ->
                "에너지 루틴이 남아 있어. 짧은 산책이나 가벼운 운동으로 컨디션을 정리하면 좋아."
        }
        val suggestedActions = buildList {
            incompleteItems.firstOrNull()?.let { add("${it.label} 체크") }
            if (incompleteItems.any { it.category == "HEALTH" }) add("건강 루틴 1개를 오늘 액션으로 전환")
            if (weeklyCompletionRate < 40) add("Daily Check를 오전/저녁 고정 체크로 묶기")
        }.distinct()

        return DailyRoutineResponse(
            date = targetDate.toString(),
            completionRate = completionRate,
            completedCount = completedCount,
            totalCount = items.size,
            streakDays = calculateStreak(targetDate),
            weeklyCompletionRate = weeklyCompletionRate,
            weeklyCompletedDays = weeklyCompletedDays,
            insight = insight,
            suggestedActions = suggestedActions,
            recentDays = recentDays,
            categoryStats = categoryStats,
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
