package com.shoptourr.insights

import com.shoptourr.TestcontainersConfiguration
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.testcontainers.junit.jupiter.Testcontainers
import tools.jackson.databind.json.JsonMapper

@Import(TestcontainersConfiguration::class)
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class InsightsApiIT {

	@Autowired
	private lateinit var mockMvc: MockMvc

	@Autowired
	private lateinit var jsonMapper: JsonMapper

	@Test
	fun `home stats alerts tax-free and route read a trip with spend`() {
		val access = token("ins-${System.nanoTime()}@example.com")
		val tripId = jsonMapper.readTree(
			mockMvc.perform(
				post("/api/trips")
					.header("Authorization", "Bearer $access")
					.contentType(MediaType.APPLICATION_JSON)
					.content(
						"""{"city":"Lisbon","country":"Portugal","countryCode":"PT","startDate":"2026-08-10","endDate":"2026-08-20","budget":{"amount":"1500.00","currency":"EUR"}}""",
					),
			).andExpect(status().isCreated).andReturn().response.contentAsByteArray,
		).get("id").stringValue()

		mockMvc.perform(
			post("/api/trips/$tripId/purchases")
				.header("Authorization", "Bearer $access")
				.contentType(MediaType.APPLICATION_JSON)
				.content(
					"""{"name":"Lunch","category":"FOOD","amount":{"amount":"40.00","currency":"EUR"},"place":"Time Out Market","taxRefundEligible":true}""",
				),
		).andExpect(status().isCreated)

		mockMvc.perform(get("/api/home").header("Authorization", "Bearer $access"))
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.user.email").exists())
			.andExpect(jsonPath("$.upcoming").isArray)

		mockMvc.perform(get("/api/trips/$tripId/stats").header("Authorization", "Bearer $access"))
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.totalSpent.amount").value("40.00"))
			.andExpect(jsonPath("$.byCategory[0].category").value("FOOD"))

		mockMvc.perform(get("/api/trips/$tripId/alerts").header("Authorization", "Bearer $access"))
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.alerts").isArray)

		mockMvc.perform(get("/api/trips/$tripId/tax-free").header("Authorization", "Bearer $access"))
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.rules.regionLabel").value("Portugal"))

		mockMvc.perform(get("/api/trips/$tripId/route").header("Authorization", "Bearer $access"))
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.stops[0].place").value("Time Out Market"))
			.andExpect(jsonPath("$.stops[0].point.lat").value("38.706900"))
			.andExpect(jsonPath("$.path[0].lng").value("-9.145700"))

		mockMvc.perform(
			post("/api/trips/$tripId/travelers")
				.header("Authorization", "Bearer $access")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"name":"Bob","colorHex":"#112233"}"""),
		)
			.andExpect(status().isCreated)
			.andExpect(jsonPath("$.name").value("Bob"))
			.andExpect(jsonPath("$.isOwner").value(false))
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
