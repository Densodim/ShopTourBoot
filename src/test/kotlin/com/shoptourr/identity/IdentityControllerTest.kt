package com.shoptourr.identity

import com.shoptourr.AuthenticationFailedException
import com.shoptourr.ResourceConflictException
import com.shoptourr.config.CorsProperties
import com.shoptourr.config.JacksonConfig
import com.shoptourr.config.SecurityConfig
import com.shoptourr.identity.dto.AuthTokensResponse
import com.shoptourr.identity.dto.AuthUserDto
import com.shoptourr.identity.dto.LoginRequest
import com.shoptourr.identity.dto.RefreshTokenRequest
import com.shoptourr.identity.dto.RegisterRequest
import com.shoptourr.web.ApiProblem
import com.shoptourr.web.ProblemAccessDeniedHandler
import com.shoptourr.web.ProblemAuthenticationEntryPoint
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Instant
import java.util.UUID

@WebMvcTest(IdentityController::class)
@Import(
	SecurityConfig::class,
	JacksonConfig::class,
	ProblemAuthenticationEntryPoint::class,
	ProblemAccessDeniedHandler::class,
)
@EnableConfigurationProperties(CorsProperties::class)
class IdentityControllerTest {

	@Autowired
	private lateinit var mockMvc: MockMvc

	@MockitoBean
	private lateinit var jwtDecoder: JwtDecoder

	@MockitoBean
	private lateinit var identityService: IdentityService

	@Test
	fun `register is public and returns 201 with tokens`() {
		`when`(identityService.register(RegisterRequest("Ada", "ada@example.com", "secret1"))).thenReturn(sampleTokens())

		mockMvc.perform(
			post("/api/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"displayName":"Ada","email":"ada@example.com","password":"secret1"}"""),
		)
			.andExpect(status().isCreated)
			.andExpect(header().string("Location", "/api/me"))
			.andExpect(jsonPath("$.accessToken").value("access"))
			.andExpect(jsonPath("$.refreshToken").value("refresh"))
			.andExpect(jsonPath("$.tokenType").value("Bearer"))
			.andExpect(jsonPath("$.user.email").value("ada@example.com"))
	}

	@Test
	fun `register rejects a short password as a validation problem`() {
		mockMvc.perform(
			post("/api/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"displayName":"Ada","email":"ada@example.com","password":"ab"}"""),
		)
			.andExpect(status().isBadRequest)
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.code").value(ApiProblem.VALIDATION_ERROR))
			.andExpect(jsonPath("$.errors[0].field").value("password"))
	}

	@Test
	fun `register conflict is a problem detail`() {
		`when`(identityService.register(RegisterRequest("Ada", "ada@example.com", "secret1"))).thenThrow(
			ResourceConflictException("An account with this email already exists."),
		)

		mockMvc.perform(
			post("/api/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"displayName":"Ada","email":"ada@example.com","password":"secret1"}"""),
		)
			.andExpect(status().isConflict)
			.andExpect(jsonPath("$.code").value(ApiProblem.CONFLICT))
	}

	@Test
	fun `login is public`() {
		`when`(identityService.login(LoginRequest("ada@example.com", "secret1"))).thenReturn(sampleTokens())

		mockMvc.perform(
			post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"email":"ada@example.com","password":"secret1"}"""),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.accessToken").value("access"))
	}

	@Test
	fun `login failure is a 401 problem detail`() {
		`when`(identityService.login(LoginRequest("ada@example.com", "nope!!"))).thenThrow(
			AuthenticationFailedException(IdentityService.INVALID_CREDENTIALS),
		)

		mockMvc.perform(
			post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"email":"ada@example.com","password":"nope!!"}"""),
		)
			.andExpect(status().isUnauthorized)
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.code").value(ApiProblem.UNAUTHORIZED))
			.andExpect(jsonPath("$.detail").value(IdentityService.INVALID_CREDENTIALS))
	}

	@Test
	fun `refresh is public`() {
		`when`(identityService.refresh(RefreshTokenRequest("r"))).thenReturn(sampleTokens())

		mockMvc.perform(
			post("/api/auth/refresh")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"refreshToken":"r"}"""),
		)
			.andExpect(status().isOk)
	}

	@Test
	fun `logout requires authentication`() {
		mockMvc.perform(post("/api/auth/logout"))
			.andExpect(status().isUnauthorized)
			.andExpect(jsonPath("$.code").value(ApiProblem.UNAUTHORIZED))
	}

	@Test
	fun `authenticated logout returns 204`() {
		mockMvc.perform(
			post("/api/auth/logout").with(
				jwt().jwt { token ->
					token.subject(UUID.randomUUID().toString())
					token.claim(TokenService.SESSION_CLAIM, UUID.randomUUID().toString())
				},
			),
		)
			.andExpect(status().isNoContent)
	}

	@Test
	fun `forgot password is public and always 204`() {
		mockMvc.perform(
			post("/api/auth/forgot-password")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"email":"ada@example.com"}"""),
		)
			.andExpect(status().isNoContent)
	}

	@Test
	fun `reset password is public and returns 204`() {
		mockMvc.perform(
			post("/api/auth/reset-password")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"email":"ada@example.com","token":"reset-token-value-ok","newPassword":"newsecret"}"""),
		)
			.andExpect(status().isNoContent)
	}

	private fun sampleTokens() = AuthTokensResponse(
		accessToken = "access",
		accessExpiresIn = 900,
		refreshToken = "refresh",
		refreshExpiresIn = 2_592_000,
		user = AuthUserDto(
			id = UUID.fromString("11111111-1111-1111-1111-111111111111"),
			displayName = "Ada",
			email = "ada@example.com",
			locale = "en",
			createdAt = Instant.parse("2026-08-13T09:00:00Z"),
		),
	)
}
