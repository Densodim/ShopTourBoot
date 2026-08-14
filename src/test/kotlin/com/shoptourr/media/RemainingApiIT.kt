package com.shoptourr.media

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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.testcontainers.junit.jupiter.Testcontainers
import tools.jackson.databind.json.JsonMapper

@Import(TestcontainersConfiguration::class)
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class RemainingApiIT {

	@Autowired
	private lateinit var mockMvc: MockMvc

	@Autowired
	private lateinit var jsonMapper: JsonMapper

	@Test
	fun `media export push invite and premium match the v1 contract`() {
		val access = token("rest-${System.nanoTime()}@example.com")

		val media = jsonMapper.readTree(
			mockMvc.perform(
				post("/api/media/upload-intents")
					.header("Authorization", "Bearer $access")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""{"purpose":"RECEIPT","contentType":"image/jpeg","byteSize":2048}"""),
			)
				.andExpect(status().isCreated)
				.andExpect(jsonPath("$.status").value("PENDING_UPLOAD"))
				.andExpect(jsonPath("$.uploadUrl").exists())
				.andReturn().response.contentAsByteArray,
		)
		val mediaId = media.get("mediaId").stringValue()
		val payload = byteArrayOf(1, 2, 3, 4)

		mockMvc.perform(
			put("/dev-uploads/$mediaId")
				.contentType(MediaType.IMAGE_JPEG)
				.content(payload),
		)
			.andExpect(status().isNoContent)

		mockMvc.perform(get("/dev-uploads/$mediaId"))
			.andExpect(status().isOk)
			.andExpect(content().bytes(payload))

		mockMvc.perform(get("/api/media/$mediaId/ocr").header("Authorization", "Bearer $access"))
			.andExpect(status().isConflict)
			.andExpect(jsonPath("$.code").value(ApiProblem.MEDIA_NOT_READY))

		mockMvc.perform(
			post("/api/media/$mediaId/confirm")
				.header("Authorization", "Bearer $access")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"uploaded":true}"""),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.status").value("READY"))
			.andExpect(jsonPath("$.downloadUrl").exists())

		mockMvc.perform(get("/api/media/$mediaId/ocr").header("Authorization", "Bearer $access"))
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.suggestedName").value("Receipt"))

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

		val export = jsonMapper.readTree(
			mockMvc.perform(
				post("/api/trips/$tripId/exports")
					.header("Authorization", "Bearer $access")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""{"format":"CSV","includeDiary":true}"""),
			)
				.andExpect(status().isAccepted)
				.andExpect(jsonPath("$.status").value("READY"))
				.andExpect(jsonPath("$.downloadUrl").exists())
				.andReturn().response.contentAsByteArray,
		)
		val exportId = export.get("id").stringValue()

		mockMvc.perform(get("/api/exports/$exportId").header("Authorization", "Bearer $access"))
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.format").value("CSV"))

		mockMvc.perform(get("/dev-exports/$exportId"))
			.andExpect(status().isOk)
			.andExpect(content().contentTypeCompatibleWith(MediaType("text", "csv")))
			.andExpect(content().string(org.hamcrest.Matchers.containsString("tax_refund_eligible")))

		mockMvc.perform(
			post("/api/trips/$tripId/invites")
				.header("Authorization", "Bearer $access")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"email":"bob@example.com","displayNameHint":"Bob"}"""),
		)
			.andExpect(status().isCreated)
			.andExpect(jsonPath("$.email").value("bob@example.com"))
			.andExpect(jsonPath("$.status").value("PENDING"))

		mockMvc.perform(
			post("/api/trips/$tripId/invites")
				.header("Authorization", "Bearer $access")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"email":"Bob@example.com"}"""),
		)
			.andExpect(status().isConflict)
			.andExpect(jsonPath("$.code").value(ApiProblem.CONFLICT))

		val device = jsonMapper.readTree(
			mockMvc.perform(
				post("/api/me/devices")
					.header("Authorization", "Bearer $access")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""{"token":"fcm-token-1","platform":"IOS","deviceName":"iPhone"}"""),
			)
				.andExpect(status().isCreated)
				.andExpect(jsonPath("$.tokenFingerprint").isString)
				.andExpect(jsonPath("$.platform").value("IOS"))
				.andReturn().response.contentAsByteArray,
		)
		val deviceId = device.get("id").stringValue()

		mockMvc.perform(delete("/api/me/devices/$deviceId").header("Authorization", "Bearer $access"))
			.andExpect(status().isNoContent)

		mockMvc.perform(
			post("/api/me/premium/activate")
				.header("Authorization", "Bearer $access")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"plan":"PLUS"}"""),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.premiumPlan").value("PLUS"))
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
			registry.add("voyage.ocr.enabled") { "false" }
			registry.add("voyage.fcm.enabled") { "false" }
		}
	}
}
