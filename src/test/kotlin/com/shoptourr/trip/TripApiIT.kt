package com.shoptourr.trip

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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.testcontainers.junit.jupiter.Testcontainers
import tools.jackson.databind.json.JsonMapper

@Import(TestcontainersConfiguration::class)
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class TripApiIT {

	@Autowired
	private lateinit var mockMvc: MockMvc

	@Autowired
	private lateinit var jsonMapper: JsonMapper

	@Test
	fun `create get list patch and delete a trip`() {
		val access = register("trip-${System.nanoTime()}@example.com")
		val created = mockMvc.perform(
			post("/api/trips")
				.header("Authorization", "Bearer $access")
				.contentType(MediaType.APPLICATION_JSON)
				.content(
					"""{"city":"Lisbon","country":"Portugal","countryCode":"PT","startDate":"2026-09-01","endDate":"2026-09-08","budget":{"amount":"1500.00","currency":"EUR"}}""",
				),
		)
			.andExpect(status().isCreated)
			.andExpect(jsonPath("$.city").value("Lisbon"))
			.andExpect(jsonPath("$.status").value("UPCOMING"))
			.andExpect(jsonPath("$.travelers[0].isOwner").value(true))
			.andReturn()
		val tripId = jsonMapper.readTree(created.response.contentAsByteArray).get("id").stringValue()

		mockMvc.perform(get("/api/trips/$tripId").header("Authorization", "Bearer $access"))
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.country").value("Portugal"))

		mockMvc.perform(get("/api/trips").header("Authorization", "Bearer $access"))
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.upcoming[0].id").value(tripId))

		mockMvc.perform(
			patch("/api/trips/$tripId")
				.header("Authorization", "Bearer $access")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"city":"Porto"}"""),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.city").value("Porto"))

		mockMvc.perform(delete("/api/trips/$tripId").header("Authorization", "Bearer $access"))
			.andExpect(status().isNoContent)

		mockMvc.perform(get("/api/trips/$tripId").header("Authorization", "Bearer $access"))
			.andExpect(status().isNotFound)
			.andExpect(jsonPath("$.code").value(ApiProblem.NOT_FOUND))
	}

	@Test
	fun `end before start is a validation problem`() {
		val access = register("bad-${System.nanoTime()}@example.com")
		mockMvc.perform(
			post("/api/trips")
				.header("Authorization", "Bearer $access")
				.contentType(MediaType.APPLICATION_JSON)
				.content(
					"""{"city":"Lisbon","country":"Portugal","startDate":"2026-09-08","endDate":"2026-09-01","budget":{"amount":"10.00","currency":"EUR"}}""",
				),
		)
			.andExpect(status().isBadRequest)
			.andExpect(jsonPath("$.code").value(ApiProblem.VALIDATION_ERROR))
	}

	private fun register(email: String): String {
		val register = mockMvc.perform(
			post("/api/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"displayName":"Ada","email":"$email","password":"secret1"}"""),
		)
			.andExpect(status().isCreated)
			.andReturn()
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
