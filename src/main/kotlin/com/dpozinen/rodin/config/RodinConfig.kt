package com.dpozinen.rodin.config

import com.dpozinen.rodin.domain.Chat
import com.dpozinen.rodin.domain.Offset
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializationContext
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
    fun redisChatConfigTemplate(factory: ReactiveRedisConnectionFactory): ReactiveRedisTemplate<String, Chat> {
        val keySerializer = StringRedisSerializer()
        val valueSerializer = Jackson2JsonRedisSerializer(jacksonObjectMapper(), Chat::class.java)

        val context = RedisSerializationContext
            .newSerializationContext<String, Chat>()
            .key(keySerializer)
            .value(valueSerializer)
            .hashKey(keySerializer)
            .hashValue(valueSerializer)
            .build()

        return ReactiveRedisTemplate(factory, context)
    }


    @Bean
    fun redisOffsetTemplate(factory: ReactiveRedisConnectionFactory): ReactiveRedisTemplate<String, Offset> {
        val keySerializer = StringRedisSerializer()
        val valueSerializer = Jackson2JsonRedisSerializer(jacksonObjectMapper(), Offset::class.java)

        val context = RedisSerializationContext
            .newSerializationContext<String, Offset>()
            .key(keySerializer)
            .value(valueSerializer)
            .hashKey(keySerializer)
            .hashValue(valueSerializer)
            .build()

        return ReactiveRedisTemplate(factory, context)
    }


}