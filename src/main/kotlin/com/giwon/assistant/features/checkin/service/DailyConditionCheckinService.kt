package com.giwon.assistant.features.checkin.service

import com.giwon.assistant.features.checkin.dto.DailyConditionCheckinResponse
import com.giwon.assistant.features.checkin.dto.DailyConditionReadinessPointResponse
import com.giwon.assistant.features.checkin.dto.UpdateDailyConditionCheckinRequest
import com.giwon.assistant.features.checkin.entity.DailyConditionCheckinEntity
import com.giwon.assistant.features.checkin.repository.DailyConditionCheckinRepository
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId

@Service
class DailyConditionCheckinService(
    private val dailyConditionCheckinRepository: DailyConditionCheckinRepository,
) {
    private val koreaZoneId: ZoneId = ZoneId.of("Asia/Seoul")

    fun getCondition(date: String?): DailyConditionCheckinResponse {
        val targetDate = date?.let(LocalDate::parse) ?: LocalDate.now(koreaZoneId)
        return buildResponse(targetDate)
    }

    fun updateCondition(date: String?, request: UpdateDailyConditionCheckinRequest): DailyConditionCheckinResponse {
        val targetDate = date?.let(LocalDate::parse) ?: LocalDate.now(koreaZoneId)
        validateScore(request.energy)
        validateScore(request.focus)
        validateScore(request.mood)
        validateScore(request.stress)
        validateScore(request.sleepQuality)

        val entity = dailyConditionCheckinRepository.findById(targetDate).orElse(
            DailyConditionCheckinEntity(checkDate = targetDate)
        )
        entity.energy = request.energy
        entity.focus = request.focus
        entity.mood = request.mood
        entity.stress = request.stress
        entity.sleepQuality = request.sleepQuality
        entity.note = request.note?.trim()?.ifBlank { null }
        entity.updatedAt = OffsetDateTime.now(koreaZoneId)
        dailyConditionCheckinRepository.save(entity)
        return buildResponse(targetDate)
    }

    private fun buildResponse(targetDate: LocalDate): DailyConditionCheckinResponse {
        val today = dailyConditionCheckinRepository.findById(targetDate).orElse(
            DailyConditionCheckinEntity(checkDate = targetDate)
        )
        val recentRangeStart = targetDate.minusDays(6)
        val recent = dailyConditionCheckinRepository.findAllByCheckDateBetween(recentRangeStart, targetDate)
            .associateBy { it.checkDate }
        val recentReadiness = (0..6).map { offset ->
            val date = recentRangeStart.plusDays(offset.toLong())
            val entity = recent[date] ?: DailyConditionCheckinEntity(checkDate = date)
            DailyConditionReadinessPointResponse(
                date = date.toString(),
                readinessScore = calculateReadiness(entity),
            )
        }
        val readinessScore = calculateReadiness(today)
        val averageReadiness = recentReadiness.map { it.readinessScore }.average()
        val trend = when {
            readinessScore >= averageReadiness + 8 -> "UP"
            readinessScore <= averageReadiness - 8 -> "DOWN"
            else -> "STABLE"
        }
        val summary = when {
            readinessScore >= 80 -> "컨디션이 안정적이야. 오늘은 긴 집중 블록을 잡아도 되는 상태다."
            readinessScore >= 60 -> "기본 컨디션은 유지되고 있어. 짧은 리셋 후 집중 작업으로 넘어가면 좋다."
            readinessScore >= 40 -> "몸 상태가 완전히 무너지진 않았지만 회복 블록을 먼저 잡는 편이 낫다."
            else -> "지금은 생산성보다 회복이 우선이야. 에너지와 수면 흐름을 먼저 복구하는 게 맞다."
        }
        val suggestions = buildList {
            if (today.sleepQuality <= 2) add("오늘은 깊은 작업보다 회복 블록을 먼저 잡기")
            if (today.stress >= 4) add("스트레스 원인 1개만 적고 처리 순서 줄이기")
            if (today.energy <= 2) add("식사나 수분 체크 후 짧은 산책 넣기")
            if (today.focus <= 2) add("25분 단위 짧은 집중 블록으로 쪼개기")
            if (today.mood <= 2) add("기분 회복용 쉬운 작업부터 하나 닫기")
            if (isEmptyState(today)) add("지금 상태를 30초 안에 빠르게 체크인하기")
        }.distinct()

        return DailyConditionCheckinResponse(
            date = targetDate.toString(),
            energy = today.energy,
            focus = today.focus,
            mood = today.mood,
            stress = today.stress,
            sleepQuality = today.sleepQuality,
            note = today.note,
            readinessScore = readinessScore,
            trend = trend,
            summary = summary,
            suggestions = suggestions,
            recentReadiness = recentReadiness,
        )
    }

    private fun calculateReadiness(entity: DailyConditionCheckinEntity): Int {
        val positive = entity.energy + entity.focus + entity.mood + entity.sleepQuality
        val stressInverted = 6 - entity.stress
        return ((positive + stressInverted) * 20) / 5
    }

    private fun isEmptyState(entity: DailyConditionCheckinEntity): Boolean =
        entity.energy == 3 &&
            entity.focus == 3 &&
            entity.mood == 3 &&
            entity.stress == 3 &&
            entity.sleepQuality == 3 &&
            entity.note.isNullOrBlank()

    private fun validateScore(value: Int) {
        require(value in 1..5) { "Score must be between 1 and 5" }
    }
}
