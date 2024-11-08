package com.dpozinen.rodin

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@EnableScheduling
@SpringBootApplication
class RodinApplication

fun main(args: Array<String>) {
	runApplication<RodinApplication>(*args)
}
