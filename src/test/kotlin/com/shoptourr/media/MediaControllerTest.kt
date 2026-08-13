package com.shoptourr.media

import com.shoptourr.config.CorsProperties
import com.shoptourr.config.JacksonConfig
import com.shoptourr.config.SecurityConfig
import com.shoptourr.media.dto.ConfirmMediaUploadRequest
import com.shoptourr.media.dto.CreateMediaUploadIntentRequest
import com.shoptourr.media.dto.MediaAssetDto
import com.shoptourr.media.dto.MediaPurpose
import com.shoptourr.media.dto.MediaStatus
import com.shoptourr.media.dto.MediaUploadIntentResponse
import com.shoptourr.media.dto.ReceiptOcrResultDto
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

@WebMvcTest(MediaController::class)
@Import(
	SecurityConfig::class,
	JacksonConfig::class,
	ProblemAuthenticationEntryPoint::class,
	ProblemAccessDeniedHandler::class,
)
@EnableConfigurationProperties(CorsProperties::class)
class MediaControllerTest {

	@Autowired
	private lateinit var mockMvc: MockMvc

	@MockitoBean
	private lateinit var jwtDecoder: JwtDecoder

	@MockitoBean
	private lateinit var mediaService: MediaService

	private val userId = UUID.fromString("11111111-1111-1111-1111-111111111111")
	private val mediaId = UUID.fromString("33333333-3333-3333-3333-333333333333")

	@Test
	fun `upload intent requires authentication`() {
		mockMvc.perform(
			post("/api/media/upload-intents")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"purpose":"RECEIPT","contentType":"image/jpeg","byteSize":1024}"""),
		)
			.andExpect(status().isUnauthorized)
			.andExpect(jsonPath("$.code").value(ApiProblem.UNAUTHORIZED))
	}

	@Test
	fun `upload intent returns 201 with location`() {
		val request = CreateMediaUploadIntentRequest(MediaPurpose.RECEIPT, "image/jpeg", 1024)
		`when`(mediaService.createIntent(eq(userId) ?: userId, any() ?: request)).thenReturn(
			MediaUploadIntentResponse(
				mediaId = mediaId,
				uploadUrl = "http://localhost:8080/dev-uploads/$mediaId",
				requiredHeaders = mapOf("Content-Type" to "image/jpeg"),
				uploadExpiresAt = Instant.parse("2026-08-13T12:15:00Z"),
				status = MediaStatus.PENDING_UPLOAD,
			),
		)

		mockMvc.perform(
			post("/api/media/upload-intents")
				.with(userJwt())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"purpose":"RECEIPT","contentType":"image/jpeg","byteSize":1024}"""),
		)
			.andExpect(status().isCreated)
			.andExpect(header().string("Location", "/api/media/$mediaId"))
			.andExpect(jsonPath("$.status").value("PENDING_UPLOAD"))
	}

	@Test
	fun `confirm returns the asset`() {
		val request = ConfirmMediaUploadRequest(true)
		`when`(mediaService.confirm(eq(userId) ?: userId, eq(mediaId) ?: mediaId, any() ?: request))
			.thenReturn(sampleAsset())

		mockMvc.perform(
			post("/api/media/$mediaId/confirm")
				.with(userJwt())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"uploaded":true}"""),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.status").value("READY"))
	}

	@Test
	fun `ocr is authenticated`() {
		`when`(mediaService.ocr(userId, mediaId)).thenReturn(
			ReceiptOcrResultDto(mediaId, "Receipt", "0.00", null, "OTHER", 0.1),
		)

		mockMvc.perform(get("/api/media/$mediaId/ocr").with(userJwt()))
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.mediaId").value(mediaId.toString()))
	}

	private fun userJwt() = jwt().jwt { it.subject(userId.toString()) }

	private fun sampleAsset() = MediaAssetDto(
		id = mediaId,
		purpose = MediaPurpose.RECEIPT,
		status = MediaStatus.READY,
		contentType = "image/jpeg",
		byteSize = 1024,
		downloadUrl = "http://localhost:8080/dev-uploads/$mediaId",
		thumbnailUrl = "http://localhost:8080/dev-uploads/$mediaId",
		createdAt = Instant.parse("2026-08-13T12:00:00Z"),
	)
}
