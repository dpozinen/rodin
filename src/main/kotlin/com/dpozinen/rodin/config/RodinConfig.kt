package com.dpozinen.rodin.config

import com.dpozinen.rodin.domain.Chat
import com.dpozinen.rodin.domain.Offset
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.StringRedisSerializer
import org.springframework.web.reactive.function.client.WebClient


@Configuration
class RodinConfig {

    @Bean
    fun webClient(
        @Value("\${rodin.telegram.host}") host: String,
        @Value("\${rodin.telegram.bot-segment}") botSegment: String,
    ) = WebClient.create("$host/$botSegment")

    @Bean
    fun redisChatConfigTemplate(factory: RedisConnectionFactory) = redisTemplate<Chat>(factory)

    @Bean
    fun redisOffsetTemplate(factory: RedisConnectionFactory) = redisTemplate<Offset>(factory)

    private inline fun <reified T> redisTemplate(factory: RedisConnectionFactory): RedisTemplate<String, T> {
        val keySerializer = StringRedisSerializer()
        val valueSerializer = Jackson2JsonRedisSerializer(jacksonObjectMapper(), T::class.java)

        return RedisTemplate<String, T>().also {
            it.connectionFactory = factory
            it.keySerializer = keySerializer
            it.valueSerializer = valueSerializer
            it.hashKeySerializer = keySerializer
            it.valueSerializer = valueSerializer
        }
    }

}