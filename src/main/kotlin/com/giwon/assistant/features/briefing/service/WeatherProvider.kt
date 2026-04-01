package com.giwon.assistant.features.briefing.service

import com.giwon.assistant.features.briefing.dto.WeatherSummary

fun interface WeatherProvider {
    fun getCurrentWeather(): WeatherSummary
}
