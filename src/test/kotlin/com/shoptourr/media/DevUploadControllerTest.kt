package com.shoptourr.media

import com.shoptourr.config.CorsProperties
import com.shoptourr.config.JacksonConfig
import com.shoptourr.config.SecurityConfig
import com.shoptourr.web.ProblemAccessDeniedHandler
import com.shoptourr.web.ProblemAuthenticationEntryPoint
import org.junit.jupiter.api.Test
import org.mockito.Mockito.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.UUID

@WebMvcTest(DevUploadController::class)
@Import(
	SecurityConfig::class,
	JacksonConfig::class,
	ProblemAuthenticationEntryPoint::class,
	ProblemAccessDeniedHandler::class,
)
@EnableConfigurationProperties(CorsProperties::class)
class DevUploadControllerTest {

	@Autowired
	private lateinit var mockMvc: MockMvc

	@MockitoBean
	private lateinit var jwtDecoder: JwtDecoder

	@MockitoBean
	private lateinit var mediaService: MediaService

	private val mediaId = UUID.fromString("33333333-3333-3333-3333-333333333333")

	@Test
	fun `put is public and stores bytes`() {
		val payload = byteArrayOf(9, 8, 7)

		mockMvc.perform(
			put("/dev-uploads/$mediaId")
				.contentType(MediaType.IMAGE_JPEG)
				.content(payload),
		)
			.andExpect(status().isNoContent)

		verify(mediaService).storeBytes(mediaId, payload)
	}

	@Test
	fun `get is public and returns stored bytes`() {
		val payload = byteArrayOf(1, 2, 3)
		org.mockito.Mockito.`when`(mediaService.loadBytes(mediaId))
			.thenReturn(StoredMedia("image/jpeg", payload))

		mockMvc.perform(get("/dev-uploads/$mediaId"))
			.andExpect(status().isOk)
			.andExpect(content().contentType(MediaType.IMAGE_JPEG))
			.andExpect(content().bytes(payload))
	}

	@Test
	fun `head returns the current upload offset`() {
		org.mockito.Mockito.`when`(mediaService.uploadOffset(mediaId)).thenReturn(256L)

		mockMvc.perform(head("/dev-uploads/$mediaId"))
			.andExpect(status().isNoContent)
			.andExpect(header().string("Upload-Offset", "256"))
			.andExpect(header().string("Tus-Resumable", "1.0.0"))
	}

	@Test
	fun `patch appends a chunk and returns the next offset`() {
		val chunk = byteArrayOf(1, 2)
		org.mockito.Mockito.`when`(
			mediaService.appendBytes(
				org.mockito.ArgumentMatchers.eq(mediaId) ?: mediaId,
				org.mockito.ArgumentMatchers.anyLong(),
				org.mockito.ArgumentMatchers.any() ?: chunk,
			),
		).thenReturn(2L)

		mockMvc.perform(
			patch("/dev-uploads/$mediaId")
				.header("Upload-Offset", "0")
				.contentType(MediaType.APPLICATION_OCTET_STREAM)
				.content(chunk),
		)
			.andExpect(status().isNoContent)
			.andExpect(header().string("Upload-Offset", "2"))
	}
}
