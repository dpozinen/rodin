package com.dpozinen.rodin

import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
    classes = [RodinApplication::class],
    properties = [
        "spring.data.redis.host=\${embedded.redis.host}",
        "spring.data.redis.port=\${embedded.redis.port}",
        "spring.data.redis.password=\${embedded.redis.password}"
    ]
)
class RodinApplicationTests {


    @Test
    fun contextLoads() {
        runBlocking {
            WebClient.create("http://localhost:8081/actuator/health")
                .get()
                .retrieve()
                .awaitBody<Map<String, *>>()
                .let { health -> assertThat(health).containsEntry("status", "UP") }
        }
    }

}
