package com.shoptourr.purchase

import com.shoptourr.config.CorsProperties
import com.shoptourr.config.JacksonConfig
import com.shoptourr.config.SecurityConfig
import com.shoptourr.purchase.dto.CreatePurchaseRequest
import com.shoptourr.purchase.dto.PurchaseCategory
import com.shoptourr.purchase.dto.PurchaseDto
import com.shoptourr.shared.dto.MoneyDto
import com.shoptourr.shared.dto.VatBreakdownDto
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

@WebMvcTest(PurchaseController::class)
@Import(
	SecurityConfig::class,
	JacksonConfig::class,
	ProblemAuthenticationEntryPoint::class,
	ProblemAccessDeniedHandler::class,
)
@EnableConfigurationProperties(CorsProperties::class)
class PurchaseControllerTest {

	@Autowired
	private lateinit var mockMvc: MockMvc

	@MockitoBean
	private lateinit var jwtDecoder: JwtDecoder

	@MockitoBean
	private lateinit var purchaseService: PurchaseService

	private val userId = UUID.fromString("11111111-1111-1111-1111-111111111111")
	private val tripId = UUID.fromString("22222222-2222-2222-2222-222222222222")

	@Test
	fun `list requires authentication`() {
		mockMvc.perform(get("/api/trips/$tripId/purchases"))
			.andExpect(status().isUnauthorized)
			.andExpect(jsonPath("$.code").value(ApiProblem.UNAUTHORIZED))
	}

	@Test
	fun `create returns 201`() {
		val request = CreatePurchaseRequest(
			name = "Coffee",
			category = PurchaseCategory.FOOD,
			amount = MoneyDto(BigDecimal("4.50"), "EUR"),
		)
		`when`(purchaseService.create(eq(userId) ?: userId, eq(tripId) ?: tripId, any() ?: request))
			.thenReturn(sample(request))

		mockMvc.perform(
			post("/api/trips/$tripId/purchases")
				.with(jwt().jwt { it.subject(userId.toString()) })
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"name":"Coffee","category":"FOOD","amount":{"amount":"4.50","currency":"EUR"}}"""),
		)
			.andExpect(status().isCreated)
			.andExpect(jsonPath("$.name").value("Coffee"))
	}

	private fun sample(request: CreatePurchaseRequest) = PurchaseDto(
		id = UUID.fromString("33333333-3333-3333-3333-333333333333"),
		tripId = tripId,
		name = request.name,
		category = request.category,
		amount = request.amount,
		vat = VatBreakdownDto(BigDecimal("3.66"), BigDecimal("0.84"), BigDecimal("4.50"), BigDecimal("23"), true),
		taxRefundEligible = false,
		place = null,
		purchaseDate = LocalDate.of(2026, 8, 13),
		purchaseTime = LocalTime.of(12, 0),
		receiptMediaId = null,
		receiptThumbnailUrl = null,
		splitWithTravelerIds = emptyList(),
		splits = emptyList(),
		yourShare = request.amount,
		quoteEquivalent = null,
		createdAt = Instant.parse("2026-08-13T12:00:00Z"),
		updatedAt = Instant.parse("2026-08-13T12:00:00Z"),
	)
}
