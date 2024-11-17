package com.dpozinen.rodin.config

import com.dpozinen.rodin.domain.Chat
import com.dpozinen.rodin.domain.Offset
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.netty.resolver.DefaultAddressResolverGroup
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.data.redis.ClientResourcesBuilderCustomizer
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
        return ReactiveRedisTemplate(factory, serializationContext<Chat>())
    }

    @Bean
    fun redisOffsetTemplate(factory: ReactiveRedisConnectionFactory): ReactiveRedisTemplate<String, Offset> {
        return ReactiveRedisTemplate(factory, serializationContext<Offset>())
    }

    private inline fun <reified T> serializationContext(): RedisSerializationContext<String, T> {
        val keySerializer = StringRedisSerializer()
        val valueSerializer = Jackson2JsonRedisSerializer(jacksonObjectMapper(), T::class.java)

        return RedisSerializationContext
            .newSerializationContext<String, T>()
            .key(keySerializer)
            .value(valueSerializer)
            .hashKey(keySerializer)
            .hashValue(valueSerializer)
            .build()
    }

}