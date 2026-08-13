package com.shoptourr.push

import com.shoptourr.config.CorsProperties
import com.shoptourr.config.JacksonConfig
import com.shoptourr.config.SecurityConfig
import com.shoptourr.push.dto.DeviceDto
import com.shoptourr.push.dto.PushPlatform
import com.shoptourr.push.dto.RegisterDeviceRequest
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Instant
import java.util.UUID

@WebMvcTest(PushController::class)
@Import(
	SecurityConfig::class,
	JacksonConfig::class,
	ProblemAuthenticationEntryPoint::class,
	ProblemAccessDeniedHandler::class,
)
@EnableConfigurationProperties(CorsProperties::class)
class PushControllerTest {

	@Autowired
	private lateinit var mockMvc: MockMvc

	@MockitoBean
	private lateinit var jwtDecoder: JwtDecoder

	@MockitoBean
	private lateinit var pushService: PushService

	private val userId = UUID.fromString("11111111-1111-1111-1111-111111111111")
	private val deviceId = UUID.fromString("55555555-5555-5555-5555-555555555555")

	@Test
	fun `register requires authentication`() {
		mockMvc.perform(
			post("/api/me/devices")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"token":"fcm-token","platform":"ANDROID"}"""),
		)
			.andExpect(status().isUnauthorized)
			.andExpect(jsonPath("$.code").value(ApiProblem.UNAUTHORIZED))
	}

	@Test
	fun `register returns 201`() {
		val request = RegisterDeviceRequest("fcm-token", PushPlatform.ANDROID)
		`when`(pushService.register(eq(userId) ?: userId, any() ?: request)).thenReturn(
			DeviceDto(
				id = deviceId,
				tokenFingerprint = "abcd1234",
				platform = PushPlatform.ANDROID,
				appVersion = "1.0",
				deviceName = "Pixel",
				createdAt = Instant.parse("2026-08-13T12:00:00Z"),
				lastSeenAt = Instant.parse("2026-08-13T12:00:00Z"),
			),
		)

		mockMvc.perform(
			post("/api/me/devices")
				.with(jwt().jwt { it.subject(userId.toString()) })
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"token":"fcm-token","platform":"ANDROID","appVersion":"1.0","deviceName":"Pixel"}"""),
		)
			.andExpect(status().isCreated)
			.andExpect(header().string("Location", "/api/me/devices/$deviceId"))
			.andExpect(jsonPath("$.platform").value("ANDROID"))
	}

	@Test
	fun `delete returns 204`() {
		mockMvc.perform(
			delete("/api/me/devices/$deviceId").with(jwt().jwt { it.subject(userId.toString()) }),
		)
			.andExpect(status().isNoContent)
	}
}
