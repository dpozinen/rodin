package com.dpozinen.rodin.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient


@Configuration
class RodinConfig {

    @Bean
    fun webClient(
        @Value("\${rodin.telegram.host}") host: String,
        @Value("\${rodin.telegram.bot-segment}") botSegment: String,
    ) = RestClient.create("$host/$botSegment")

}