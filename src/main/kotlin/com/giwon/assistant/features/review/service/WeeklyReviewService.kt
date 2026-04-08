package com.giwon.assistant.features.review.service

import com.giwon.assistant.features.action.repository.CopilotActionRepository
import com.giwon.assistant.features.copilot.repository.CopilotHistoryRepository
import com.giwon.assistant.features.idea.repository.IdeaRepository
import com.giwon.assistant.features.review.dto.WeeklyReviewMetrics
import com.giwon.assistant.features.review.dto.WeeklyReviewResponse
import org.springframework.stereotype.Service
import java.time.OffsetDateTime

@Service
class WeeklyReviewService(
    private val copilotHistoryRepository: CopilotHistoryRepository,
    private val copilotActionRepository: CopilotActionRepository,
    private val ideaRepository: IdeaRepository,
) {
    fun getWeeklyReview(): WeeklyReviewResponse {
        val periodEnd = OffsetDateTime.now()
        val periodStart = periodEnd.minusDays(7)

        val histories = copilotHistoryRepository.findAll()
            .filter { !it.generatedAt.isBefore(periodStart) }
            .sortedByDescending { it.generatedAt }
        val actions = copilotActionRepository.findAll()
            .filter { !it.createdAt.isBefore(periodStart) }
            .sortedByDescending { it.createdAt }
        val ideas = ideaRepository.findAll()
            .filter { !it.createdAt.isBefore(periodStart) }
            .sortedByDescending { it.createdAt }

        val completedActions = actions.count { it.status == "DONE" }
        val openActions = actions.count { it.status == "OPEN" }
        val topQuestion = histories.groupingBy { it.question }.eachCount().maxByOrNull { it.value }?.key
        val latestIdeaTitle = ideas.firstOrNull()?.title

        val metrics = WeeklyReviewMetrics(
            questionsAsked = histories.size,
            actionsCreated = actions.size,
            actionsCompleted = completedActions,
            openActions = openActions,
            ideasCaptured = ideas.size,
        )

        val wins = buildList {
            if (completedActions > 0) add("이번 주 완료 액션 ${completedActions}건으로 실행 흐름이 유지됐다.")
            if (ideas.isNotEmpty()) add("아이디어 ${ideas.size}건을 기록해 다음 작업 후보를 쌓았다.")
            if (histories.isNotEmpty()) add("코파일럿 질문 ${histories.size}건으로 우선순위 판단 데이터를 남겼다.")
        }.ifEmpty {
            listOf("이번 주에는 아직 회고 데이터가 많지 않아 다음 주부터 질문/액션 로그를 더 쌓는 게 좋다.")
        }

        val risks = buildList {
            if (openActions > 0) add("미완료 액션 ${openActions}건이 남아 있어 다음 주 시작점이 분산될 수 있다.")
            if (completedActions == 0 && actions.isNotEmpty()) add("액션 생성 대비 완료가 없어 실행 전환이 약했다.")
            if (topQuestion != null) add("가장 많이 반복한 질문은 '${topQuestion}'였다. 같은 판단을 반복하고 있을 수 있다.")
        }.ifEmpty {
            listOf("리스크는 크지 않지만, 질문과 액션을 더 꾸준히 남겨야 회고 품질이 올라간다.")
        }

        val nextFocus = buildList {
            latestIdeaTitle?.let { add("최근 아이디어 '${it}'를 다음 주 첫 검토 대상으로 올리기") }
            if (openActions > 0) add("열린 액션 ${openActions}건 중 상위 1건만 월요일 첫 블록에 배치")
            add("반복 질문 1건을 없앨 수 있도록 템플릿 또는 체크리스트로 고정")
        }

        return WeeklyReviewResponse(
            periodStart = periodStart.toString(),
            periodEnd = periodEnd.toString(),
            summary = buildSummary(metrics, completedActions, topQuestion),
            metrics = metrics,
            wins = wins,
            risks = risks,
            nextFocus = nextFocus,
        )
    }

    private fun buildSummary(
        metrics: WeeklyReviewMetrics,
        completedActions: Int,
        topQuestion: String?,
    ): String {
        val actionPart = if (completedActions > 0) {
            "액션 ${metrics.actionsCreated}건 중 ${completedActions}건을 완료했다."
        } else {
            "액션은 ${metrics.actionsCreated}건 쌓였고 실행 완료는 아직 없다."
        }
        val questionPart = topQuestion?.let { "가장 자주 반복된 질문은 '${it}'였다." }
            ?: "질문 로그가 많지 않아 패턴 분석은 다음 주에 더 정확해질 수 있다."
        return "지난 7일 동안 코파일럿 질문 ${metrics.questionsAsked}건, 아이디어 ${metrics.ideasCaptured}건이 쌓였다. $actionPart $questionPart"
    }
}
