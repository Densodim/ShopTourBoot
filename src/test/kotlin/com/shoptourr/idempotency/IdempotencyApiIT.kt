package com.shoptourr.idempotency

import com.shoptourr.TestcontainersConfiguration
import com.shoptourr.web.ApiProblem
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.testcontainers.junit.jupiter.Testcontainers
import tools.jackson.databind.json.JsonMapper

@Import(TestcontainersConfiguration::class)
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class IdempotencyApiIT {

	@Autowired
	private lateinit var mockMvc: MockMvc

	@Autowired
	private lateinit var jsonMapper: JsonMapper

	@Test
	fun `create POSTs replay the original response for the same key and reject a different body`() {
		val access = token("idem-${System.nanoTime()}@example.com")
		val body = """{"city":"Lisbon","country":"Portugal","countryCode":"PT","startDate":"2026-09-01","endDate":"2026-09-08","budget":{"amount":"1500.00","currency":"EUR"}}"""

		val first = mockMvc.perform(
			post("/api/trips")
				.header("Authorization", "Bearer $access")
				.header(IdempotencyService.HEADER, "trip-create-1")
				.contentType(MediaType.APPLICATION_JSON)
				.content(body),
		)
			.andExpect(status().isCreated)
			.andExpect(jsonPath("$.city").value("Lisbon"))
			.andReturn()
		val tripId = jsonMapper.readTree(first.response.contentAsByteArray).get("id").stringValue()

		mockMvc.perform(
			post("/api/trips")
				.header("Authorization", "Bearer $access")
				.header(IdempotencyService.HEADER, "trip-create-1")
				.contentType(MediaType.APPLICATION_JSON)
				.content(body),
		)
			.andExpect(status().isCreated)
			.andExpect(jsonPath("$.id").value(tripId))
			.andExpect(jsonPath("$.city").value("Lisbon"))

		mockMvc.perform(
			post("/api/trips")
				.header("Authorization", "Bearer $access")
				.header(IdempotencyService.HEADER, "trip-create-1")
				.contentType(MediaType.APPLICATION_JSON)
				.content(body.replace("Lisbon", "Porto")),
		)
			.andExpect(status().isConflict)
			.andExpect(jsonPath("$.code").value(ApiProblem.IDEMPOTENCY_CONFLICT))
	}

	private fun token(email: String): String {
		val register = mockMvc.perform(
			post("/api/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"displayName":"Ada","email":"$email","password":"secret1"}"""),
		).andExpect(status().isCreated).andReturn()
		return jsonMapper.readTree(register.response.contentAsByteArray).get("accessToken").stringValue()
	}

	companion object {
		@JvmStatic
		@DynamicPropertySource
		fun jwtProps(registry: DynamicPropertyRegistry) {
			registry.add("voyage.jwt.secret") { "test-only-secret-key-32bytes-min!!" }
		}
	}
}
