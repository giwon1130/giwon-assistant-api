package com.giwon.assistant

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class GiwonAssistantApiApplication

fun main(args: Array<String>) {
    runApplication<GiwonAssistantApiApplication>(*args)
}
