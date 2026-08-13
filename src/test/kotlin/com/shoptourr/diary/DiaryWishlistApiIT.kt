package com.shoptourr.diary

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
class DiaryWishlistApiIT {

	@Autowired
	private lateinit var mockMvc: MockMvc

	@Autowired
	private lateinit var jsonMapper: JsonMapper

	@Test
	fun `diary and wishlist round-trip`() {
		val access = token("life-${System.nanoTime()}@example.com")
		val tripId = jsonMapper.readTree(
			mockMvc.perform(
				post("/api/trips")
					.header("Authorization", "Bearer $access")
					.contentType(MediaType.APPLICATION_JSON)
					.content(
						"""{"city":"Lisbon","country":"Portugal","startDate":"2026-08-10","endDate":"2026-08-20","budget":{"amount":"1500.00","currency":"EUR"}}""",
					),
			).andExpect(status().isCreated).andReturn().response.contentAsByteArray,
		).get("id").stringValue()

		val diary = mockMvc.perform(
			post("/api/trips/$tripId/diary")
				.header("Authorization", "Bearer $access")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"mood":"😊","text":"Pastéis and sunshine"}"""),
		)
			.andExpect(status().isCreated)
			.andExpect(jsonPath("$.text").value("Pastéis and sunshine"))
			.andReturn()
		val entryId = jsonMapper.readTree(diary.response.contentAsByteArray).get("id").stringValue()

		mockMvc.perform(
			patch("/api/trips/$tripId/diary/$entryId")
				.header("Authorization", "Bearer $access")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"text":"Updated note"}"""),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.text").value("Updated note"))

		mockMvc.perform(get("/api/trips/$tripId/diary").header("Authorization", "Bearer $access"))
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.days[0].entries[0].id").value(entryId))

		val wish = mockMvc.perform(
			post("/api/wishlist")
				.header("Authorization", "Bearer $access")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"name":"Tile","city":"Lisbon","targetPrice":{"amount":"25.00","currency":"EUR"}}"""),
		)
			.andExpect(status().isCreated)
			.andReturn()
		val wishId = jsonMapper.readTree(wish.response.contentAsByteArray).get("id").stringValue()

		mockMvc.perform(get("/api/wishlist").header("Authorization", "Bearer $access"))
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.items[0].id").value(wishId))

		mockMvc.perform(delete("/api/wishlist/$wishId").header("Authorization", "Bearer $access"))
			.andExpect(status().isNoContent)
		mockMvc.perform(delete("/api/trips/$tripId/diary/$entryId").header("Authorization", "Bearer $access"))
			.andExpect(status().isNoContent)
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
