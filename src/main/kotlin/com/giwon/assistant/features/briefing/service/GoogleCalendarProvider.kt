package com.giwon.assistant.features.briefing.service

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.giwon.assistant.features.briefing.dto.CalendarItem
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@Component
@ConditionalOnProperty(
    prefix = "assistant.integrations",
    name = ["calendar-enabled"],
    havingValue = "true",
)
class GoogleCalendarProvider(
    private val calendarProperties: AssistantCalendarProperties,
    @Qualifier("googleCalendarRestClient") private val calendarRestClient: RestClient,
    @Qualifier("googleOAuthRestClient") private val oAuthRestClient: RestClient,
) : CalendarProvider {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun getEvents(date: LocalDate): List<CalendarItem> {
        val accessToken = fetchAccessToken() ?: run {
            log.warn("Failed to fetch Google access token, falling back to defaults")
            return calendarProperties.defaultEvents.map { CalendarItem(time = it.time, title = it.title) }
        }

        val zone = ZoneId.of(calendarProperties.timezone)
        val timeMin = date.atStartOfDay(zone).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        val timeMax = date.plusDays(1).atStartOfDay(zone).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)

        return runCatching {
            val response = calendarRestClient.get()
                .uri { builder ->
                    builder.path("/calendar/v3/calendars/{calendarId}/events")
                        .queryParam("timeMin", timeMin)
                        .queryParam("timeMax", timeMax)
                        .queryParam("singleEvents", "true")
                        .queryParam("orderBy", "startTime")
                        .build(calendarProperties.calendarId)
                }
                .header("Authorization", "Bearer $accessToken")
                .retrieve()
                .body(GoogleCalendarEventsResponse::class.java)

            response?.items?.mapNotNull { it.toCalendarItem(zone) } ?: emptyList()
        }.getOrElse {
            log.warn("Failed to fetch Google Calendar events: ${it.message}")
            calendarProperties.defaultEvents.map { event -> CalendarItem(time = event.time, title = event.title, mock = true) }
        }
    }

    private fun fetchAccessToken(): String? =
        runCatching {
            val body = "grant_type=refresh_token" +
                "&client_id=${calendarProperties.clientId}" +
                "&client_secret=${calendarProperties.clientSecret}" +
                "&refresh_token=${calendarProperties.refreshToken}"

            val response = oAuthRestClient.post()
                .uri("/token")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .body(body)
                .retrieve()
                .body(GoogleTokenResponse::class.java)

            response?.accessToken
        }.getOrElse {
            log.warn("Google OAuth token refresh failed: ${it.message}")
            null
        }

    private fun GoogleCalendarEvent.toCalendarItem(zone: ZoneId): CalendarItem? {
        val startTime = start?.dateTime?.let {
            runCatching {
                ZonedDateTime.parse(it).withZoneSameInstant(zone)
                    .format(DateTimeFormatter.ofPattern("HH:mm"))
            }.getOrNull()
        } ?: start?.date?.let { "종일" } ?: return null

        return CalendarItem(time = startTime, title = summary ?: "(제목 없음)")
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class GoogleCalendarEventsResponse(
    val items: List<GoogleCalendarEvent>? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class GoogleCalendarEvent(
    val summary: String? = null,
    val start: GoogleCalendarEventTime? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class GoogleCalendarEventTime(
    val dateTime: String? = null,
    val date: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class GoogleTokenResponse(
    @JsonProperty("access_token") val accessToken: String? = null,
)
