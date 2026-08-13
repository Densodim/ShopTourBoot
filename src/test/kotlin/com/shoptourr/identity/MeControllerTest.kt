package com.shoptourr.identity

import com.shoptourr.config.CorsProperties
import com.shoptourr.config.JacksonConfig
import com.shoptourr.config.SecurityConfig
import com.shoptourr.identity.dto.PremiumPlan
import com.shoptourr.identity.dto.ThemePreference
import com.shoptourr.identity.dto.UserDto
import com.shoptourr.identity.dto.UserPreferencesDto
import com.shoptourr.identity.dto.UserStatsDto
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Instant
import java.util.UUID

@WebMvcTest(MeController::class)
@Import(
	SecurityConfig::class,
	JacksonConfig::class,
	ProblemAuthenticationEntryPoint::class,
	ProblemAccessDeniedHandler::class,
)
@EnableConfigurationProperties(CorsProperties::class)
class MeControllerTest {

	@Autowired
	private lateinit var mockMvc: MockMvc

	@MockitoBean
	private lateinit var jwtDecoder: JwtDecoder

	@MockitoBean
	private lateinit var meService: MeService

	private val userId = UUID.fromString("11111111-1111-1111-1111-111111111111")

	@Test
	fun `get me requires authentication`() {
		mockMvc.perform(get("/api/me"))
			.andExpect(status().isUnauthorized)
			.andExpect(jsonPath("$.code").value(ApiProblem.UNAUTHORIZED))
	}

	@Test
	fun `get me returns the current profile`() {
		`when`(meService.getMe(userId)).thenReturn(sampleUser())

		mockMvc.perform(get("/api/me").with(userJwt()))
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.email").value("ada@example.com"))
			.andExpect(jsonPath("$.stats.tripsCount").value(0))
	}

	@Test
	fun `patch me updates the display name`() {
		`when`(meService.updateProfile(userId, com.shoptourr.identity.dto.UpdateProfileRequest("Ada Lovelace")))
			.thenReturn(sampleUser().copy(displayName = "Ada Lovelace"))

		mockMvc.perform(
			patch("/api/me")
				.with(userJwt())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"displayName":"Ada Lovelace"}"""),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.displayName").value("Ada Lovelace"))
	}

	@Test
	fun `get preferences requires authentication`() {
		mockMvc.perform(get("/api/me/preferences"))
			.andExpect(status().isUnauthorized)
	}

	@Test
	fun `get preferences returns settings`() {
		`when`(meService.getPreferences(userId)).thenReturn(
			UserPreferencesDto("en", "EUR", ThemePreference.DARK, true, true),
		)

		mockMvc.perform(get("/api/me/preferences").with(userJwt()))
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.preferredCurrency").value("EUR"))
			.andExpect(jsonPath("$.theme").value("DARK"))
	}

	@Test
	fun `app config is authenticated`() {
		mockMvc.perform(get("/api/me/app-config"))
			.andExpect(status().isUnauthorized)
	}

	@Test
	fun `activate premium returns the updated plan`() {
		`when`(meService.activatePremium(userId, com.shoptourr.identity.dto.ActivatePremiumRequest(PremiumPlan.PLUS)))
			.thenReturn(sampleUser().copy(premiumPlan = PremiumPlan.PLUS))

		mockMvc.perform(
			post("/api/me/premium/activate")
				.with(userJwt())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"plan":"PLUS"}"""),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.premiumPlan").value("PLUS"))
	}

	private fun userJwt() = jwt().jwt { it.subject(userId.toString()) }

	private fun sampleUser() = UserDto(
		id = userId,
		displayName = "Ada",
		email = "ada@example.com",
		avatarUrl = null,
		locale = "en",
		preferredCurrency = "RUB",
		theme = ThemePreference.SYSTEM,
		pushNotificationsEnabled = true,
		memberSince = Instant.parse("2026-01-01T00:00:00Z"),
		premiumPlan = PremiumPlan.FREE,
		stats = UserStatsDto(0, 0, 0),
	)
}
