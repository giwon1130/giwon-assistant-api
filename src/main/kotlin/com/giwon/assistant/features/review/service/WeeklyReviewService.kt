package com.giwon.assistant.features.review.service

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.giwon.assistant.features.action.repository.CopilotActionRepository
import com.giwon.assistant.features.briefing.service.AssistantAnthropicProperties
import com.giwon.assistant.features.copilot.repository.CopilotHistoryRepository
import com.giwon.assistant.features.idea.repository.IdeaRepository
import com.giwon.assistant.features.routine.service.DailyRoutineService
import com.giwon.assistant.features.review.dto.WeeklyReviewMetrics
import com.giwon.assistant.features.review.dto.WeeklyReviewResponse
import com.giwon.assistant.features.review.dto.WeeklyReviewSnapshotResponse
import com.giwon.assistant.features.review.entity.WeeklyReviewSnapshotEntity
import com.giwon.assistant.features.review.repository.WeeklyReviewSnapshotRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import java.time.DayOfWeek
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.temporal.TemporalAdjusters

@Service
class WeeklyReviewService(
    private val copilotHistoryRepository: CopilotHistoryRepository,
    private val copilotActionRepository: CopilotActionRepository,
    private val ideaRepository: IdeaRepository,
    private val dailyRoutineService: DailyRoutineService,
    private val weeklyReviewSnapshotRepository: WeeklyReviewSnapshotRepository,
    private val objectMapper: ObjectMapper,
    @Qualifier("claudeRestClient") private val claudeRestClient: RestClient,
    private val anthropicProperties: AssistantAnthropicProperties,
    @Value("\${assistant.integrations.claude-enabled:false}") private val claudeEnabled: Boolean,
    @Value("\${ANTHROPIC_API_KEY:}") private val anthropicApiKey: String,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    fun getWeeklyReview(): WeeklyReviewResponse {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val periodStart = now.toLocalDate()
            .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            .atStartOfDay()
            .atOffset(ZoneOffset.UTC)
        val periodEnd = periodStart.plusDays(7)

        val histories = copilotHistoryRepository.findAll()
            .filter { !it.generatedAt.isBefore(periodStart) && it.generatedAt.isBefore(periodEnd) }
            .sortedByDescending { it.generatedAt }
        val actions = copilotActionRepository.findAll()
            .filter { !it.createdAt.isBefore(periodStart) && it.createdAt.isBefore(periodEnd) }
            .sortedByDescending { it.createdAt }
        val ideas = ideaRepository.findAll()
            .filter { !it.createdAt.isBefore(periodStart) && it.createdAt.isBefore(periodEnd) }
            .sortedByDescending { it.createdAt }

        val completedActions = actions.count { it.status == "DONE" }
        val openActions = actions.count { it.status == "OPEN" }
        val topQuestion = histories.groupingBy { it.question }.eachCount().maxByOrNull { it.value }?.key
        val latestIdeaTitle = ideas.firstOrNull()?.title
        val routineChecksCompleted = dailyRoutineService.getCompletedChecksBetween(periodStart.toLocalDate(), periodEnd.toLocalDate())
        val routineCompletionDays = dailyRoutineService.getCompletedDaysBetween(periodStart.toLocalDate(), periodEnd.toLocalDate())

        val metrics = WeeklyReviewMetrics(
            questionsAsked = histories.size,
            actionsCreated = actions.size,
            actionsCompleted = completedActions,
            openActions = openActions,
            ideasCaptured = ideas.size,
            routineChecksCompleted = routineChecksCompleted,
            routineCompletionDays = routineCompletionDays,
        )

        val wins = buildList {
            if (completedActions > 0) add("이번 주 완료 액션 ${completedActions}건으로 실행 흐름이 유지됐다.")
            if (ideas.isNotEmpty()) add("아이디어 ${ideas.size}건을 기록해 다음 작업 후보를 쌓았다.")
            if (histories.isNotEmpty()) add("코파일럿 질문 ${histories.size}건으로 우선순위 판단 데이터를 남겼다.")
            if (routineChecksCompleted > 0) add("루틴 체크 ${routineChecksCompleted}건으로 생활 리듬을 기록했다.")
        }.ifEmpty {
            listOf("이번 주에는 아직 회고 데이터가 많지 않아 다음 주부터 질문/액션 로그를 더 쌓는 게 좋다.")
        }

        val risks = buildList {
            if (openActions > 0) add("미완료 액션 ${openActions}건이 남아 있어 다음 주 시작점이 분산될 수 있다.")
            if (completedActions == 0 && actions.isNotEmpty()) add("액션 생성 대비 완료가 없어 실행 전환이 약했다.")
            if (topQuestion != null) add("가장 많이 반복한 질문은 '${topQuestion}'였다. 같은 판단을 반복하고 있을 수 있다.")
            if (routineCompletionDays == 0) add("루틴 체크 완료일이 없어 생활 리듬 데이터가 거의 남지 않았다.")
        }.ifEmpty {
            listOf("리스크는 크지 않지만, 질문과 액션을 더 꾸준히 남겨야 회고 품질이 올라간다.")
        }

        val nextFocus = buildList {
            latestIdeaTitle?.let { add("최근 아이디어 '${it}'를 다음 주 첫 검토 대상으로 올리기") }
            if (openActions > 0) add("열린 액션 ${openActions}건 중 상위 1건만 월요일 첫 블록에 배치")
            if (routineCompletionDays < 3) add("비타민/수분/수면 루틴 중 1개만이라도 다음 주 기본 체크로 고정")
            add("반복 질문 1건을 없앨 수 있도록 템플릿 또는 체크리스트로 고정")
        }

        val ruleBased = WeeklyReviewResponse(
            periodStart = periodStart.toString(),
            periodEnd = periodEnd.toString(),
            summary = buildSummary(metrics, completedActions, topQuestion),
            metrics = metrics,
            wins = wins,
            risks = risks,
            nextFocus = nextFocus,
        )

        val review = enrichWithClaude(ruleBased) ?: ruleBased
        persistSnapshot(review)
        return review
    }

    fun getWeeklyReviewHistory(): List<WeeklyReviewSnapshotResponse> =
        weeklyReviewSnapshotRepository.findTop8ByOrderByGeneratedAtDesc()
            .map { it.toResponse() }

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
        val routinePart = if (metrics.routineChecksCompleted > 0) {
            "루틴 체크는 ${metrics.routineChecksCompleted}건 기록됐다."
        } else {
            "루틴 체크 데이터는 아직 거의 없다."
        }
        val questionPart = topQuestion?.let { "가장 자주 반복된 질문은 '${it}'였다." }
            ?: "질문 로그가 많지 않아 패턴 분석은 다음 주에 더 정확해질 수 있다."
        return "이번 주 코파일럿 질문 ${metrics.questionsAsked}건, 아이디어 ${metrics.ideasCaptured}건이 쌓였다. $actionPart $routinePart $questionPart"
    }

    private fun enrichWithClaude(review: WeeklyReviewResponse): WeeklyReviewResponse? {
        if (!claudeEnabled || anthropicApiKey.isBlank()) return null

        return runCatching {
            val prompt = """
            다음은 이번 주 개인 생산성 데이터다. 이를 바탕으로 주간 리뷰를 작성해줘.
            반드시 아래 형식만 사용해.

            SUMMARY: 이번 주를 2~3문장으로 요약
            WINS:
            - 잘한 점 1
            - 잘한 점 2
            - 잘한 점 3
            RISKS:
            - 리스크/아쉬운 점 1
            - 리스크/아쉬운 점 2
            NEXT_FOCUS:
            - 다음 주 집중 포인트 1
            - 다음 주 집중 포인트 2
            - 다음 주 집중 포인트 3

            이번 주 데이터:
            - 코파일럿 질문: ${review.metrics.questionsAsked}건
            - 생성된 액션: ${review.metrics.actionsCreated}건
            - 완료된 액션: ${review.metrics.actionsCompleted}건
            - 미완료 액션: ${review.metrics.openActions}건
            - 캡처된 아이디어: ${review.metrics.ideasCaptured}건
            - 루틴 체크 완료: ${review.metrics.routineChecksCompleted}건
            - 루틴 완료일: ${review.metrics.routineCompletionDays}일
            기존 요약: ${review.summary}
            """.trimIndent()

            val body = mapOf(
                "model" to anthropicProperties.model,
                "max_tokens" to 512,
                "messages" to listOf(mapOf("role" to "user", "content" to prompt)),
            )

            val responseBody = claudeRestClient.post()
                .uri("/v1/messages")
                .header("x-api-key", anthropicApiKey)
                .header("anthropic-version", "2023-06-01")
                .body(body)
                .retrieve()
                .body(String::class.java)
                ?: error("Claude response is empty")

            val root = objectMapper.readTree(responseBody)
            val outputText = root.path("content")
                .takeIf { it.isArray }
                ?.firstOrNull { it.path("type").asText() == "text" }
                ?.path("text")?.asText()
                ?.takeIf { it.isNotBlank() }
                ?: error("Claude content is empty")

            parseClaudeReview(outputText, review)
        }.getOrElse {
            log.warn("Claude weekly review enrichment failed: ${it.message}")
            null
        }
    }

    private fun parseClaudeReview(text: String, fallback: WeeklyReviewResponse): WeeklyReviewResponse {
        val lines = text.lines().map { it.trim() }.filter { it.isNotBlank() }
        val summary = lines.firstOrNull { it.startsWith("SUMMARY:") }
            ?.substringAfter("SUMMARY:")?.trim()
            ?: fallback.summary
        val wins = extractBulletSection(lines, "WINS:").ifEmpty { fallback.wins }
        val risks = extractBulletSection(lines, "RISKS:").ifEmpty { fallback.risks }
        val nextFocus = extractBulletSection(lines, "NEXT_FOCUS:").ifEmpty { fallback.nextFocus }

        return fallback.copy(summary = summary, wins = wins, risks = risks, nextFocus = nextFocus)
    }

    private fun extractBulletSection(lines: List<String>, header: String): List<String> {
        val startIndex = lines.indexOfFirst { it == header }
        if (startIndex == -1) return emptyList()
        return lines.drop(startIndex + 1)
            .takeWhile { !it.endsWith(":") }
            .mapNotNull { line ->
                when {
                    line.startsWith("- ") -> line.removePrefix("- ").trim()
                    line.startsWith("* ") -> line.removePrefix("* ").trim()
                    else -> null
                }
            }
    }

    private fun persistSnapshot(review: WeeklyReviewResponse) {
        val periodStart = OffsetDateTime.parse(review.periodStart)
        val periodEnd = OffsetDateTime.parse(review.periodEnd)
        val snapshotId = "WEEKLY-${periodStart.toLocalDate()}"
        val existing = weeklyReviewSnapshotRepository.findById(snapshotId).orElse(null)
        val entity = existing ?: WeeklyReviewSnapshotEntity(
            id = snapshotId,
            periodStart = periodStart,
            periodEnd = periodEnd,
        )

        entity.summary = review.summary
        entity.metrics = objectMapper.writeValueAsString(review.metrics)
        entity.wins = objectMapper.writeValueAsString(review.wins)
        entity.risks = objectMapper.writeValueAsString(review.risks)
        entity.nextFocus = objectMapper.writeValueAsString(review.nextFocus)
        entity.generatedAt = OffsetDateTime.now()
        weeklyReviewSnapshotRepository.save(entity)
    }

    private fun WeeklyReviewSnapshotEntity.toResponse(): WeeklyReviewSnapshotResponse =
        WeeklyReviewSnapshotResponse(
            id = id,
            periodStart = periodStart.toString(),
            periodEnd = periodEnd.toString(),
            summary = summary,
            metrics = objectMapper.readValue(metrics, WeeklyReviewMetrics::class.java),
            wins = objectMapper.readValue(wins, objectMapper.typeFactory.constructCollectionType(List::class.java, String::class.java)),
            risks = objectMapper.readValue(risks, objectMapper.typeFactory.constructCollectionType(List::class.java, String::class.java)),
            nextFocus = objectMapper.readValue(nextFocus, objectMapper.typeFactory.constructCollectionType(List::class.java, String::class.java)),
            generatedAt = generatedAt.toString(),
        )
}
