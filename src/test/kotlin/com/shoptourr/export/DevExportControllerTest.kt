package com.shoptourr.export

import com.shoptourr.ResourceNotFoundException
import com.shoptourr.config.CorsProperties
import com.shoptourr.config.JacksonConfig
import com.shoptourr.config.SecurityConfig
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
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.UUID

@WebMvcTest(DevExportController::class)
@Import(
	SecurityConfig::class,
	JacksonConfig::class,
	ProblemAuthenticationEntryPoint::class,
	ProblemAccessDeniedHandler::class,
)
@EnableConfigurationProperties(CorsProperties::class)
class DevExportControllerTest {

	@Autowired
	private lateinit var mockMvc: MockMvc

	@MockitoBean
	private lateinit var jwtDecoder: JwtDecoder

	@MockitoBean
	private lateinit var exportService: ExportService

	private val exportId = UUID.fromString("44444444-4444-4444-4444-444444444444")

	@Test
	fun `get is public and returns the file`() {
		val body = "id,name\n".toByteArray()
		`when`(exportService.loadFile(exportId)).thenReturn(
			StoredExport("export-$exportId.csv", "text/csv; charset=UTF-8", body),
		)

		mockMvc.perform(get("/dev-exports/$exportId"))
			.andExpect(status().isOk)
			.andExpect(content().contentTypeCompatibleWith(MediaType("text", "csv")))
			.andExpect(header().string("Content-Disposition", "attachment; filename=\"export-$exportId.csv\""))
			.andExpect(content().bytes(body))
	}

	@Test
	fun `missing export is not found`() {
		`when`(exportService.loadFile(exportId)).thenThrow(ResourceNotFoundException("Export not found."))

		mockMvc.perform(get("/dev-exports/$exportId"))
			.andExpect(status().isNotFound)
			.andExpect(jsonPath("$.code").value(ApiProblem.NOT_FOUND))
	}
}
