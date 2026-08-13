package com.shoptourr.identity

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
class IdentityApiIT {

	@Autowired
	private lateinit var mockMvc: MockMvc

	@Autowired
	private lateinit var jsonMapper: JsonMapper

	@Test
	fun `register login refresh and logout form a closed session loop`() {
		val email = "loop-${System.nanoTime()}@example.com"
		val register = mockMvc.perform(
			post("/api/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"displayName":"Ada","email":"$email","password":"secret1","locale":"en"}"""),
		)
			.andExpect(status().isCreated)
			.andExpect(jsonPath("$.user.email").value(email))
			.andExpect(jsonPath("$.tokenType").value("Bearer"))
			.andReturn()

		val registered = jsonMapper.readTree(register.response.contentAsByteArray)
		val refreshToken = registered.get("refreshToken").stringValue()
		val accessToken = registered.get("accessToken").stringValue()

		mockMvc.perform(
			post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"email":"$email","password":"secret1"}"""),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.user.email").value(email))

		val refreshed = mockMvc.perform(
			post("/api/auth/refresh")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"refreshToken":"$refreshToken"}"""),
		)
			.andExpect(status().isOk)
			.andReturn()
		val newRefresh = jsonMapper.readTree(refreshed.response.contentAsByteArray).get("refreshToken").stringValue()

		mockMvc.perform(
			post("/api/auth/refresh")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"refreshToken":"$refreshToken"}"""),
		)
			.andExpect(status().isUnauthorized)
			.andExpect(jsonPath("$.code").value(ApiProblem.UNAUTHORIZED))

		mockMvc.perform(
			post("/api/auth/logout")
				.header("Authorization", "Bearer $accessToken")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"refreshToken":"$newRefresh"}"""),
		)
			.andExpect(status().isNoContent)

		mockMvc.perform(
			post("/api/auth/refresh")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"refreshToken":"$newRefresh"}"""),
		)
			.andExpect(status().isUnauthorized)
	}

	@Test
	fun `registering the same email twice is a conflict`() {
		val email = "dup-${System.nanoTime()}@example.com"
		val body = """{"displayName":"Ada","email":"$email","password":"secret1"}"""

		mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
			.andExpect(status().isCreated)
		mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
			.andExpect(status().isConflict)
			.andExpect(jsonPath("$.code").value(ApiProblem.CONFLICT))
	}

	@Test
	fun `forgot password is always 204`() {
		mockMvc.perform(
			post("/api/auth/forgot-password")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"email":"nobody-${System.nanoTime()}@example.com"}"""),
		)
			.andExpect(status().isNoContent)
	}

	companion object {
		@JvmStatic
		@DynamicPropertySource
		fun jwtProps(registry: DynamicPropertyRegistry) {
			registry.add("voyage.jwt.secret") { "test-only-secret-key-32bytes-min!!" }
		}
	}
}
