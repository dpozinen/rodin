package com.dpozinen.rodin

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(
	webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
	classes = [RodinApplication::class]
)
class RodinApplicationTests {

	@Test
	fun contextLoads() {
	}

}
