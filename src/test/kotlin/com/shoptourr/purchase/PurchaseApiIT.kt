package com.shoptourr.purchase

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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
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
class PurchaseApiIT {

	@Autowired
	private lateinit var mockMvc: MockMvc

	@Autowired
	private lateinit var jsonMapper: JsonMapper

	@Test
	fun `create list get and delete a purchase on a trip`() {
		val access = register("buy-${System.nanoTime()}@example.com")
		val trip = mockMvc.perform(
			post("/api/trips")
				.header("Authorization", "Bearer $access")
				.contentType(MediaType.APPLICATION_JSON)
				.content(
					"""{"city":"Lisbon","country":"Portugal","countryCode":"PT","startDate":"2026-08-10","endDate":"2026-08-20","budget":{"amount":"1500.00","currency":"EUR"},"defaultVatRatePercent":23}""",
				),
		)
			.andExpect(status().isCreated)
			.andReturn()
		val tripId = jsonMapper.readTree(trip.response.contentAsByteArray).get("id").stringValue()

		val created = mockMvc.perform(
			post("/api/trips/$tripId/purchases")
				.header("Authorization", "Bearer $access")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"name":"Pastel de nata","category":"FOOD","amount":{"amount":"123.00","currency":"EUR"},"vatIncluded":true}"""),
		)
			.andExpect(status().isCreated)
			.andExpect(jsonPath("$.name").value("Pastel de nata"))
			.andExpect(jsonPath("$.vat.net").value("100.00"))
			.andReturn()
		val purchaseId = jsonMapper.readTree(created.response.contentAsByteArray).get("id").stringValue()

		mockMvc.perform(get("/api/trips/$tripId/purchases").header("Authorization", "Bearer $access"))
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.spentTotal.amount").value("123.00"))
			.andExpect(jsonPath("$.days[0].items[0].id").value(purchaseId))

		mockMvc.perform(get("/api/trips/$tripId").header("Authorization", "Bearer $access"))
			.andExpect(jsonPath("$.spent.amount").value("123.00"))
			.andExpect(jsonPath("$.purchaseCount").value(1))

		mockMvc.perform(delete("/api/trips/$tripId/purchases/$purchaseId").header("Authorization", "Bearer $access"))
			.andExpect(status().isNoContent)

		mockMvc.perform(get("/api/trips/$tripId/purchases/$purchaseId").header("Authorization", "Bearer $access"))
			.andExpect(status().isNotFound)
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
