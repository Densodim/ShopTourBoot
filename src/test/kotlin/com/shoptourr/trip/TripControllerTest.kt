package com.shoptourr.trip

import com.shoptourr.config.CorsProperties
import com.shoptourr.config.JacksonConfig
import com.shoptourr.config.SecurityConfig
import com.shoptourr.shared.dto.MoneyDto
import com.shoptourr.trip.dto.CreateTripRequest
import com.shoptourr.trip.dto.TripDto
import com.shoptourr.trip.dto.TripListResponse
import com.shoptourr.trip.dto.TripStatus
import com.shoptourr.web.ApiProblem
import com.shoptourr.web.ProblemAccessDeniedHandler
import com.shoptourr.web.ProblemAuthenticationEntryPoint
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@WebMvcTest(TripController::class)
@Import(
	SecurityConfig::class,
	JacksonConfig::class,
	ProblemAuthenticationEntryPoint::class,
	ProblemAccessDeniedHandler::class,
)
@EnableConfigurationProperties(CorsProperties::class)
class TripControllerTest {

	@Autowired
	private lateinit var mockMvc: MockMvc

	@MockitoBean
	private lateinit var jwtDecoder: JwtDecoder

	@MockitoBean
	private lateinit var tripService: TripService

	private val userId = UUID.fromString("11111111-1111-1111-1111-111111111111")
	private val tripId = UUID.fromString("22222222-2222-2222-2222-222222222222")

	@Test
	fun `list requires authentication`() {
		mockMvc.perform(get("/api/trips"))
			.andExpect(status().isUnauthorized)
			.andExpect(jsonPath("$.code").value(ApiProblem.UNAUTHORIZED))
	}

	@Test
	fun `list returns buckets`() {
		`when`(tripService.list(userId)).thenReturn(TripListResponse(emptyList(), emptyList(), emptyList()))

		mockMvc.perform(get("/api/trips").with(userJwt()))
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.active").isArray)
	}

	@Test
	fun `create returns 201 with location`() {
		val request = CreateTripRequest(
			city = "Lisbon",
			country = "Portugal",
			countryCode = "PT",
			startDate = LocalDate.of(2026, 9, 1),
			endDate = LocalDate.of(2026, 9, 8),
			budget = MoneyDto(BigDecimal("1500.00"), "EUR"),
		)
		`when`(tripService.create(eq(userId) ?: userId, any() ?: request)).thenReturn(sampleTrip())

		mockMvc.perform(
			post("/api/trips")
				.with(userJwt())
				.contentType(MediaType.APPLICATION_JSON)
				.content(
					"""{"city":"Lisbon","country":"Portugal","countryCode":"PT","startDate":"2026-09-01","endDate":"2026-09-08","budget":{"amount":"1500.00","currency":"EUR"}}""",
				),
		)
			.andExpect(status().isCreated)
			.andExpect(header().string("Location", "/api/trips/$tripId"))
			.andExpect(jsonPath("$.city").value("Lisbon"))
			.andExpect(jsonPath("$.budget.amount").value("1500.00"))
	}

	@Test
	fun `delete returns 204`() {
		mockMvc.perform(delete("/api/trips/$tripId").with(userJwt()))
			.andExpect(status().isNoContent)
	}

	@Test
	fun `invite returns 201`() {
		val inviteId = UUID.fromString("66666666-6666-6666-6666-666666666666")
		val request = com.shoptourr.trip.dto.InviteTravelerRequest("bob@example.com")
		`when`(tripService.inviteTraveler(eq(userId) ?: userId, eq(tripId) ?: tripId, any() ?: request))
			.thenReturn(
				com.shoptourr.trip.dto.TripInviteDto(
					id = inviteId,
					tripId = tripId,
					email = "bob@example.com",
					status = com.shoptourr.trip.dto.TripInviteStatus.PENDING,
					createdAt = Instant.parse("2026-08-13T12:00:00Z"),
					expiresAt = Instant.parse("2026-08-20T12:00:00Z"),
				),
			)

		mockMvc.perform(
			post("/api/trips/$tripId/invites")
				.with(userJwt())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"email":"bob@example.com"}"""),
		)
			.andExpect(status().isCreated)
			.andExpect(jsonPath("$.status").value("PENDING"))
	}

	private fun userJwt() = jwt().jwt { it.subject(userId.toString()) }

	private fun sampleTrip() = TripDto(
		id = tripId,
		city = "Lisbon",
		country = "Portugal",
		countryCode = "PT",
		flagEmoji = "🇵🇹",
		status = TripStatus.UPCOMING,
		startDate = LocalDate.of(2026, 9, 1),
		endDate = LocalDate.of(2026, 9, 8),
		datesLabel = "1–8 SEP",
		budget = MoneyDto(BigDecimal("1500.00"), "EUR"),
		spent = MoneyDto(BigDecimal.ZERO, "EUR"),
		remaining = MoneyDto(BigDecimal("1500.00"), "EUR"),
		purchaseCount = 0,
		dayCount = 8,
		currentDayNumber = null,
		defaultVatRatePercent = BigDecimal.ZERO,
		exchangeRate = null,
		travelers = emptyList(),
		createdAt = Instant.parse("2026-08-13T12:00:00Z"),
		updatedAt = Instant.parse("2026-08-13T12:00:00Z"),
	)
}
