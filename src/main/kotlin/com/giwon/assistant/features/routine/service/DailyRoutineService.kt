package com.giwon.assistant.features.routine.service

import com.giwon.assistant.features.routine.dto.DailyRoutineCategoryStatResponse
import com.giwon.assistant.features.routine.dto.DailyRoutineDaySummaryResponse
import com.giwon.assistant.features.routine.dto.DailyRoutineFocusModeResponse
import com.giwon.assistant.features.routine.dto.DailyRoutineItemResponse
import com.giwon.assistant.features.routine.dto.DailyRoutineReminderResponse
import com.giwon.assistant.features.routine.dto.DailyRoutineResponse
import com.giwon.assistant.features.routine.dto.DailyRoutineSignalResponse
import com.giwon.assistant.features.routine.dto.UpdateDailyRoutineRequest
import com.giwon.assistant.features.routine.entity.DailyRoutineCheckEntity
import com.giwon.assistant.features.routine.repository.DailyRoutineCheckRepository
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.math.roundToInt

@Service
class DailyRoutineService(
    private val dailyRoutineCheckRepository: DailyRoutineCheckRepository,
) {
    private val koreaZoneId: ZoneId = ZoneId.of("Asia/Seoul")

    fun getDailyRoutine(date: String?): DailyRoutineResponse {
        val targetDate = date?.let(LocalDate::parse) ?: LocalDate.now(koreaZoneId)
        return buildResponse(targetDate)
    }

    fun updateDailyRoutine(date: String?, itemKey: String, request: UpdateDailyRoutineRequest): DailyRoutineResponse {
        val targetDate = date?.let(LocalDate::parse) ?: LocalDate.now(koreaZoneId)
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
        val now = OffsetDateTime.now(koreaZoneId)
        val isToday = targetDate == now.toLocalDate()
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
        val categoryCompletionRate = categoryStats.associate { stat ->
            stat.category to if (stat.totalCount == 0) 0.0 else stat.completedCount.toDouble() / stat.totalCount
        }
        val incompleteItems = items.filter { !it.completed }
        val energyScore = (
            (categoryCompletionRate["NUTRITION"] ?: 0.0) * 0.35 +
                (categoryCompletionRate["HEALTH"] ?: 0.0) * 0.35 +
                (categoryCompletionRate["ENERGY"] ?: 0.0) * 0.30
            ) * 100
        val recoveryScore = (
            (categoryCompletionRate["RECOVERY"] ?: 0.0) * 0.45 +
                (weeklyCompletionRate / 100.0) * 0.35 +
                (minOf(streakDaysBaseline(targetDate), 7) / 7.0) * 0.20
            ) * 100
        val routineRiskLevel = when {
            completionRate < 25 || (isToday && now.hour >= 20 && incompleteItems.size >= 5) -> "HIGH"
            completionRate < 55 || incompleteItems.any { it.category == "HEALTH" || it.category == "NUTRITION" } -> "MEDIUM"
            else -> "LOW"
        }
        val insight = when {
            completedCount == items.size ->
                "오늘 루틴을 전부 체크했어. 지금은 이 흐름을 내일까지 유지하는 게 핵심이야."
            incompleteItems.any { it.category == "NUTRITION" } ->
                "식사 로그가 비어 있어. 아침, 점심, 저녁 중 비어 있는 끼니부터 먼저 체크해두는 게 좋아."
            incompleteItems.any { it.category == "HEALTH" } ->
                "건강 루틴이 아직 남아 있어. 비타민, 물, 약 복용 같은 기본 항목부터 먼저 닫는 게 좋아."
            incompleteItems.any { it.category == "RECOVERY" } ->
                "회복 루틴이 비어 있어. 밤 루틴과 수면 준비를 오늘 마감 루틴으로 고정하는 편이 좋다."
            else ->
                "에너지 루틴이 남아 있어. 짧은 산책이나 가벼운 운동으로 컨디션을 정리하면 좋아."
        }
        val suggestedActions = buildList {
            incompleteItems.firstOrNull()?.let { add("${it.label} 체크") }
            if (incompleteItems.any { it.category == "NUTRITION" }) add("비어 있는 식사 로그 1개 먼저 기록")
            if (incompleteItems.any { it.category == "HEALTH" }) add("건강 루틴 1개를 오늘 액션으로 전환")
            if (weeklyCompletionRate < 40) add("Daily Check를 오전/저녁 고정 체크로 묶기")
        }.distinct()
        val signals = buildSignals(
            categoryCompletionRate = categoryCompletionRate,
            incompleteItems = incompleteItems,
            currentHour = if (isToday) now.hour else 12,
        )
        val focusMode = buildFocusMode(
            incompleteItems = incompleteItems,
            riskLevel = routineRiskLevel,
            energyScore = energyScore.roundToInt(),
            recoveryScore = recoveryScore.roundToInt(),
        )
        val reminders = incompleteItems.take(4).map { item ->
            DailyRoutineReminderResponse(
                itemKey = item.key,
                label = item.label,
                reminderTime = buildReminderTime(item.targetTime),
                reason = buildReminderReason(item.category, item.targetTime),
            )
        }

        return DailyRoutineResponse(
            date = targetDate.toString(),
            completionRate = completionRate,
            completedCount = completedCount,
            totalCount = items.size,
            streakDays = calculateStreak(targetDate),
            weeklyCompletionRate = weeklyCompletionRate,
            weeklyCompletedDays = weeklyCompletedDays,
            energyScore = energyScore.roundToInt(),
            recoveryScore = recoveryScore.roundToInt(),
            riskLevel = routineRiskLevel,
            insight = insight,
            suggestedActions = suggestedActions,
            signals = signals,
            focusMode = focusMode,
            reminders = reminders,
            recentDays = recentDays,
            categoryStats = categoryStats,
            items = items,
        )
    }

    private fun buildReminderTime(targetTime: String): String =
        when (targetTime) {
            "아침" -> "09:00"
            "점심" -> "12:30"
            "오후" -> "15:00"
            "저녁" -> "18:30"
            "밤" -> "22:00"
            "하루" -> "20:00"
            "아침/저녁" -> "19:00"
            else -> "18:00"
        }

    private fun buildReminderReason(category: String, targetTime: String): String =
        when (category) {
            "NUTRITION" -> "식사 로그가 비면 하루 컨디션 기록이 끊기기 쉬워서 ${targetTime} 전에 한 번 더 체크하는 게 좋다."
            "HEALTH" -> "건강 루틴은 하루가 밀리면 연속성이 끊겨서 ${targetTime} 기준으로 다시 챙기는 편이 좋다."
            "RECOVERY" -> "회복 루틴은 밤으로 밀릴수록 빠뜨리기 쉬워서 고정 리마인더가 필요하다."
            else -> "에너지 루틴은 한 번 놓치면 그대로 지나가기 쉬워서 ${targetTime} 전에 다시 보는 게 좋다."
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

    private fun streakDaysBaseline(targetDate: LocalDate): Int = calculateStreak(targetDate)

    private fun buildSignals(
        categoryCompletionRate: Map<String, Double>,
        incompleteItems: List<DailyRoutineItemResponse>,
        currentHour: Int,
    ): List<DailyRoutineSignalResponse> {
        val nutritionRate = (categoryCompletionRate["NUTRITION"] ?: 0.0)
        val healthRate = (categoryCompletionRate["HEALTH"] ?: 0.0)
        val energyRate = (categoryCompletionRate["ENERGY"] ?: 0.0)
        val recoveryRate = (categoryCompletionRate["RECOVERY"] ?: 0.0)

        return listOf(
            DailyRoutineSignalResponse(
                label = "Nutrition Pulse",
                status = when {
                    nutritionRate >= 0.67 -> "GOOD"
                    currentHour >= 14 && nutritionRate == 0.0 -> "ALERT"
                    else -> "WATCH"
                },
                detail = when {
                    nutritionRate >= 0.67 -> "식사 흐름이 안정적이야. 에너지 저하 리스크가 낮다."
                    currentHour >= 14 && nutritionRate == 0.0 -> "식사 로그가 비어 있어. 오후 집중력 저하 가능성이 크다."
                    else -> "식사 로그가 일부 비어 있어. 한 끼만 더 정리하면 흐름이 안정된다."
                },
            ),
            DailyRoutineSignalResponse(
                label = "Health Baseline",
                status = when {
                    healthRate >= 0.67 -> "GOOD"
                    incompleteItems.any { it.key == "MEDICATION" } && currentHour >= 19 -> "ALERT"
                    else -> "WATCH"
                },
                detail = when {
                    healthRate >= 0.67 -> "비타민, 수분, 약 복용 루틴이 기본선을 지키고 있어."
                    incompleteItems.any { it.key == "MEDICATION" } && currentHour >= 19 -> "복용 체크가 비어 있어. 오늘 건강 루틴에서 가장 먼저 닫아야 할 항목이다."
                    else -> "건강 루틴이 일부 비어 있어. 물이나 복용 기록부터 닫는 편이 좋다."
                },
            ),
            DailyRoutineSignalResponse(
                label = "Energy Output",
                status = when {
                    energyRate >= 0.5 -> "GOOD"
                    currentHour >= 18 && energyRate == 0.0 -> "WATCH"
                    else -> "READY"
                },
                detail = when {
                    energyRate >= 0.5 -> "산책이나 운동이 들어가 있어서 집중 전환용 에너지가 확보돼 있어."
                    currentHour >= 18 && energyRate == 0.0 -> "움직임 루틴이 아직 비어 있어. 짧은 산책만 해도 컨디션 회복에 도움 된다."
                    else -> "아직 움직임 루틴을 넣을 시간 여유가 있어. 오후 블록에 짧게 끼워 넣으면 된다."
                },
            ),
            DailyRoutineSignalResponse(
                label = "Recovery Window",
                status = when {
                    recoveryRate >= 1.0 -> "GOOD"
                    currentHour >= 21 -> "WATCH"
                    else -> "READY"
                },
                detail = when {
                    recoveryRate >= 1.0 -> "수면 준비 루틴이 잡혀 있어 밤 흐름이 안정적이다."
                    currentHour >= 21 -> "회복 루틴을 늦게 시작하면 내일 시작점이 무거워질 수 있다."
                    else -> "회복 루틴은 아직 준비 단계야. 밤 루틴 시간을 미리 고정해두면 좋아."
                },
            ),
        )
    }

    private fun buildFocusMode(
        incompleteItems: List<DailyRoutineItemResponse>,
        riskLevel: String,
        energyScore: Int,
        recoveryScore: Int,
    ): DailyRoutineFocusModeResponse {
        val nutritionPending = incompleteItems.any { it.category == "NUTRITION" }
        val healthPending = incompleteItems.any { it.category == "HEALTH" }
        val recoveryPending = incompleteItems.any { it.category == "RECOVERY" }

        return when {
            riskLevel == "HIGH" || nutritionPending || healthPending ->
                DailyRoutineFocusModeResponse(
                    title = "Reset Block",
                    durationMinutes = 20,
                    summary = "먹는 것과 건강 체크를 먼저 닫아서 오늘 컨디션 하한선을 복구하는 짧은 정리 블록이 필요해.",
                    trigger = "식사/건강 루틴 누락이 남아 있을 때",
                )

            recoveryPending || recoveryScore < 45 ->
                DailyRoutineFocusModeResponse(
                    title = "Recovery Prep",
                    durationMinutes = 30,
                    summary = "밤 루틴을 당기고 수면 준비를 먼저 해두는 편이 내일 집중도를 지키는 데 유리해.",
                    trigger = "회복 점수가 낮거나 수면 준비가 비어 있을 때",
                )

            energyScore < 55 ->
                DailyRoutineFocusModeResponse(
                    title = "Energy Reload",
                    durationMinutes = 25,
                    summary = "짧은 산책이나 가벼운 운동으로 몸을 먼저 깨우고, 그 다음 집중 작업으로 넘어가는 게 좋다.",
                    trigger = "움직임 루틴이 비어 있고 에너지 점수가 낮을 때",
                )

            else ->
                DailyRoutineFocusModeResponse(
                    title = "Deep Focus Window",
                    durationMinutes = 50,
                    summary = "기본 루틴이 안정적이라서 지금은 긴 집중 블록을 잡아도 되는 상태야.",
                    trigger = "기본 루틴이 일정 수준 이상 채워졌을 때",
                )
        }
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
            RoutineSpec("BREAKFAST", "아침 식사", "아침 식사를 챙겼는지 기록", "NUTRITION", "아침"),
            RoutineSpec("LUNCH", "점심 식사", "점심 식사를 놓치지 않았는지 기록", "NUTRITION", "점심"),
            RoutineSpec("DINNER", "저녁 식사", "저녁 식사 또는 가벼운 저녁을 챙겼는지 기록", "NUTRITION", "저녁"),
            RoutineSpec("VITAMIN", "비타민", "아침에 영양제나 비타민을 챙겨 먹었는지 체크", "HEALTH", "아침"),
            RoutineSpec("WATER", "물 2L", "수분 섭취 목표를 채웠는지 체크", "HEALTH", "하루"),
            RoutineSpec("WORKOUT", "운동", "가벼운 운동이나 스트레칭을 했는지 체크", "ENERGY", "저녁"),
            RoutineSpec("WALK", "산책", "짧게라도 걷기 루틴을 지켰는지 체크", "ENERGY", "오후"),
            RoutineSpec("MEDICATION", "약 복용", "필요한 약을 놓치지 않았는지 체크", "HEALTH", "아침/저녁"),
            RoutineSpec("SLEEP", "수면 준비", "자기 전 수면 루틴을 챙겼는지 체크", "RECOVERY", "밤"),
        )
    }
}
