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
        "spring.datasource.url=jdbc:postgresql://\${embedded.postgresql.host}:\${embedded.postgresql.port}/\${embedded.postgresql.schema}",
        "spring.datasource.username=\${embedded.postgresql.user}",
        "spring.datasource.password=\${embedded.postgresql.password}"
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
