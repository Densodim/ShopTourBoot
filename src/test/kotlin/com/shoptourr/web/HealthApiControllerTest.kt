package com.shoptourr.web

import com.shoptourr.config.CorsProperties
import com.shoptourr.config.JacksonConfig
import com.shoptourr.config.SecurityConfig
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * Guards the parts of the web layer that are easy to break silently: which routes are public,
 * what an unauthenticated call looks like on the wire, the CORS allow-list, and the correlation
 * header. All of it runs without Docker.
 */
@WebMvcTest(HealthApiController::class)
@Import(SecurityConfig::class, JacksonConfig::class, ProblemAuthenticationEntryPoint::class, ProblemAccessDeniedHandler::class)
@EnableConfigurationProperties(CorsProperties::class)
class HealthApiControllerTest {

	@Autowired
	private lateinit var mockMvc: MockMvc

	@MockitoBean
	private lateinit var jwtDecoder: JwtDecoder

	@Test
	fun `ping is public and returns ok`() {
		mockMvc.perform(get("/api/_ping"))
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.status").value("ok"))
	}

	@Test
	fun `every response carries a request id`() {
		mockMvc.perform(get("/api/_ping"))
			.andExpect(header().exists(RequestIdFilter.HEADER))
	}

	@Test
	fun `an incoming request id is echoed back unchanged`() {
		mockMvc.perform(get("/api/_ping").header(RequestIdFilter.HEADER, "trace-me-42"))
			.andExpect(header().string(RequestIdFilter.HEADER, "trace-me-42"))
	}

	@Test
	fun `unauthenticated call to a protected path returns a problem detail`() {
		mockMvc.perform(get("/api/protected-thing"))
			.andExpect(status().isUnauthorized)
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.status").value(401))
			.andExpect(jsonPath("$.title").value("Unauthorized"))
			.andExpect(jsonPath("$.code").value(ApiProblem.UNAUTHORIZED))
			.andExpect(jsonPath("$.type").value("https://api.shoptourr.com/problems/unauthorized"))
	}

	@Test
	fun `authenticated call to an unknown path returns a not found problem detail`() {
		mockMvc.perform(get("/api/protected-thing").with(jwt()))
			.andExpect(status().isNotFound)
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.code").value(ApiProblem.NOT_FOUND))
	}

	@Test
	fun `a 404 does not leak Spring's static-resource wording`() {
		mockMvc.perform(get("/api/protected-thing").with(jwt()))
			.andExpect(jsonPath("$.detail").value("No endpoint matches this request."))
	}

	@Test
	fun `preflight from an allowed origin is accepted`() {
		mockMvc.perform(
			options("/api/_ping")
				.header(HttpHeaders.ORIGIN, "http://localhost:5173")
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"),
		)
			.andExpect(status().isOk)
			.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"))
	}

	@Test
	fun `preflight from an unknown origin is rejected`() {
		mockMvc.perform(
			options("/api/_ping")
				.header(HttpHeaders.ORIGIN, "https://not-our-frontend.example.com")
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"),
		)
			.andExpect(status().isForbidden)
			.andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN))
	}

	@Test
	fun `a rejected preflight follows the error contract like everything else`() {
		mockMvc.perform(
			options("/api/_ping")
				.header(HttpHeaders.ORIGIN, "https://not-our-frontend.example.com")
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"),
		)
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.code").value(ApiProblem.FORBIDDEN))
			.andExpect(jsonPath("$.status").value(403))
	}
}
