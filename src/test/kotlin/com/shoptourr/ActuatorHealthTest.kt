package com.shoptourr

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.http.HttpStatus
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.web.client.RestClient
import org.testcontainers.junit.jupiter.Testcontainers

/**
 * Health has to be green on a machine with no SMTP server, otherwise the signal is worthless:
 * a permanently red aggregate teaches everyone to ignore it. Postgres and Redis are real
 * containers here, so this still fails if either of them is genuinely broken.
 */
@Import(TestcontainersConfiguration::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers(disabledWithoutDocker = true)
class ActuatorHealthTest {

	@Value("\${local.server.port}")
	private var port: Int = 0

	private fun get(path: String) =
		RestClient.create()
			.get()
			.uri("http://localhost:$port$path")
			.retrieve()
			.onStatus({ true }, { _, _ -> })
			.toEntity(String::class.java)

	@Test
	fun `health is UP and reachable without a token`() {
		val response = get("/actuator/health")

		assertEquals(HttpStatus.OK, response.statusCode)
		assertTrue(response.body?.contains("\"status\":\"UP\"") == true, response.body)
	}

	@Test
	fun `liveness and readiness probes are UP`() {
		assertEquals(HttpStatus.OK, get("/actuator/health/liveness").statusCode)
		assertEquals(HttpStatus.OK, get("/actuator/health/readiness").statusCode)
	}

	companion object {
		@JvmStatic
		@DynamicPropertySource
		fun jwtProps(registry: DynamicPropertyRegistry) {
			registry.add("voyage.jwt.secret") {
				"test-only-secret-key-32bytes-min!!"
			}
		}
	}
}
