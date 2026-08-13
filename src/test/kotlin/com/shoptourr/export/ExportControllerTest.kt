package com.shoptourr.export

import com.shoptourr.config.CorsProperties
import com.shoptourr.config.JacksonConfig
import com.shoptourr.config.SecurityConfig
import com.shoptourr.export.dto.CreateExportRequest
import com.shoptourr.export.dto.ExportFormat
import com.shoptourr.export.dto.ExportJobDto
import com.shoptourr.export.dto.ExportJobStatus
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
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Instant
import java.util.UUID

@WebMvcTest(ExportController::class)
@Import(
	SecurityConfig::class,
	JacksonConfig::class,
	ProblemAuthenticationEntryPoint::class,
	ProblemAccessDeniedHandler::class,
)
@EnableConfigurationProperties(CorsProperties::class)
class ExportControllerTest {

	@Autowired
	private lateinit var mockMvc: MockMvc

	@MockitoBean
	private lateinit var jwtDecoder: JwtDecoder

	@MockitoBean
	private lateinit var exportService: ExportService

	private val userId = UUID.fromString("11111111-1111-1111-1111-111111111111")
	private val tripId = UUID.fromString("22222222-2222-2222-2222-222222222222")
	private val exportId = UUID.fromString("44444444-4444-4444-4444-444444444444")

	@Test
	fun `create export requires authentication`() {
		mockMvc.perform(
			post("/api/trips/$tripId/exports")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"format":"PDF"}"""),
		)
			.andExpect(status().isUnauthorized)
			.andExpect(jsonPath("$.code").value(ApiProblem.UNAUTHORIZED))
	}

	@Test
	fun `create export returns 202 with location`() {
		val request = CreateExportRequest(ExportFormat.PDF)
		`when`(exportService.create(eq(userId) ?: userId, eq(tripId) ?: tripId, any() ?: request))
			.thenReturn(sampleJob())

		mockMvc.perform(
			post("/api/trips/$tripId/exports")
				.with(jwt().jwt { it.subject(userId.toString()) })
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"format":"PDF","includeTaxFree":true}"""),
		)
			.andExpect(status().isAccepted)
			.andExpect(header().string("Location", "/api/exports/$exportId"))
			.andExpect(jsonPath("$.status").value("READY"))
	}

	@Test
	fun `get export is authenticated`() {
		`when`(exportService.get(userId, exportId)).thenReturn(sampleJob())

		mockMvc.perform(
			get("/api/exports/$exportId").with(jwt().jwt { it.subject(userId.toString()) }),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.format").value("PDF"))
	}

	private fun sampleJob() = ExportJobDto(
		id = exportId,
		tripId = tripId,
		format = ExportFormat.PDF,
		status = ExportJobStatus.READY,
		downloadUrl = "http://localhost:8080/dev-exports/$exportId",
		expiresAt = Instant.parse("2026-08-14T12:00:00Z"),
		errorCode = null,
		createdAt = Instant.parse("2026-08-13T12:00:00Z"),
		finishedAt = Instant.parse("2026-08-13T12:00:00Z"),
	)
}
