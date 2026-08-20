package com.shoptourr.analytics

import com.shoptourr.analytics.dto.AnalyticsBatchRequest
import com.shoptourr.analytics.dto.AnalyticsEventIngestDto
import com.shoptourr.config.CorsProperties
import com.shoptourr.config.JacksonConfig
import com.shoptourr.config.SecurityConfig
import com.shoptourr.web.ApiProblem
import com.shoptourr.web.ProblemAccessDeniedHandler
import com.shoptourr.web.ProblemAuthenticationEntryPoint
import org.junit.jupiter.api.Test
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
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
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.UUID

@WebMvcTest(AnalyticsController::class)
@Import(
	SecurityConfig::class,
	JacksonConfig::class,
	ProblemAuthenticationEntryPoint::class,
	ProblemAccessDeniedHandler::class,
)
@EnableConfigurationProperties(CorsProperties::class)
class AnalyticsControllerTest {

	@Autowired
	private lateinit var mockMvc: MockMvc

	@MockitoBean
	private lateinit var jwtDecoder: JwtDecoder

	@MockitoBean
	private lateinit var analyticsService: AnalyticsService

	private val userId = UUID.fromString("11111111-1111-1111-1111-111111111111")

	@Test
	fun `ingest requires authentication`() {
		mockMvc.perform(
			post("/api/me/analytics-events")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"events":[{"id":"e1","name":"home_opened","timestamp":"2026-08-20T08:00:00Z"}]}"""),
		)
			.andExpect(status().isUnauthorized)
			.andExpect(jsonPath("$.code").value(ApiProblem.UNAUTHORIZED))
	}

	@Test
	fun `ingest returns 204`() {
		mockMvc.perform(
			post("/api/me/analytics-events")
				.with(jwt().jwt { it.subject(userId.toString()) })
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"events":[{"id":"e1","name":"home_opened","properties":{"tab":"home"},"timestamp":"2026-08-20T08:00:00Z"}],"userId":"ignored"}"""),
		)
			.andExpect(status().isNoContent)

		verify(analyticsService).ingest(
			userId,
			AnalyticsBatchRequest(
				events = listOf(
					AnalyticsEventIngestDto(
						id = "e1",
						name = "home_opened",
						properties = mapOf("tab" to "home"),
						timestamp = "2026-08-20T08:00:00Z",
					),
				),
				userId = "ignored",
			),
		)
	}

	@Test
	fun `ingest rejects an empty batch`() {
		mockMvc.perform(
			post("/api/me/analytics-events")
				.with(jwt().jwt { it.subject(userId.toString()) })
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"events":[]}"""),
		)
			.andExpect(status().isBadRequest)
			.andExpect(jsonPath("$.code").value(ApiProblem.VALIDATION_ERROR))

		verifyNoInteractions(analyticsService)
	}
}
