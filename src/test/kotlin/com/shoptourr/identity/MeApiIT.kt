package com.shoptourr.identity

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
class MeApiIT {

	@Autowired
	private lateinit var mockMvc: MockMvc

	@Autowired
	private lateinit var jsonMapper: JsonMapper

	@Test
	fun `registered user can read and patch profile and preferences`() {
		val email = "me-${System.nanoTime()}@example.com"
		val register = mockMvc.perform(
			post("/api/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"displayName":"Ada","email":"$email","password":"secret1","locale":"en"}"""),
		)
			.andExpect(status().isCreated)
			.andReturn()
		val access = jsonMapper.readTree(register.response.contentAsByteArray).get("accessToken").stringValue()

		mockMvc.perform(get("/api/me").header("Authorization", "Bearer $access"))
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.email").value(email))
			.andExpect(jsonPath("$.displayName").value("Ada"))
			.andExpect(jsonPath("$.locale").value("en"))
			.andExpect(jsonPath("$.premiumPlan").value("FREE"))
			.andExpect(jsonPath("$.stats.tripsCount").value(0))

		mockMvc.perform(
			patch("/api/me")
				.header("Authorization", "Bearer $access")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"displayName":"Ada Lovelace"}"""),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.displayName").value("Ada Lovelace"))

		mockMvc.perform(
			patch("/api/me/preferences")
				.header("Authorization", "Bearer $access")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"preferredCurrency":"EUR","theme":"DARK"}"""),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.preferredCurrency").value("EUR"))
			.andExpect(jsonPath("$.theme").value("DARK"))
			.andExpect(jsonPath("$.locale").value("en"))

		mockMvc.perform(get("/api/me/app-config").header("Authorization", "Bearer $access"))
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.minAndroidBuild").value(1))
			.andExpect(jsonPath("$.flags.exportPdf").value(true))

		mockMvc.perform(get("/api/me/app-config"))
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.minIosBuild").value(1))

		mockMvc.perform(delete("/api/me").header("Authorization", "Bearer $access"))
			.andExpect(status().isNoContent)

		mockMvc.perform(get("/api/me").header("Authorization", "Bearer $access"))
			.andExpect(status().isNotFound)
	}

	companion object {
		@JvmStatic
		@DynamicPropertySource
		fun jwtProps(registry: DynamicPropertyRegistry) {
			registry.add("voyage.jwt.secret") { "test-only-secret-key-32bytes-min!!" }
		}
	}
}
