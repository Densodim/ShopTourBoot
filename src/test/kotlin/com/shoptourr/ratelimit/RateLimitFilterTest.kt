package com.shoptourr.ratelimit

import com.shoptourr.config.RateLimitProperties
import com.shoptourr.web.ApiProblem
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.springframework.http.HttpHeaders
import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import tools.jackson.databind.json.JsonMapper
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class RateLimitFilterTest {

	private val rateLimits = org.mockito.Mockito.mock(RateLimitService::class.java)
	private val clock = Clock.fixed(Instant.parse("2026-08-13T12:00:00Z"), ZoneOffset.UTC)
	private val properties = RateLimitProperties(authPerWindow = 20, apiPerWindow = 300)
	private val filter = RateLimitFilter(rateLimits, properties, JsonMapper.builder().build(), clock)

	@Test
	fun `ping is not rate limited`() {
		val request = MockHttpServletRequest("GET", "/api/_ping")

		filter.doFilter(request, MockHttpServletResponse(), MockFilterChain())

		verifyNoInteractions(rateLimits)
	}

	@Test
	fun `oauth uses the auth bucket`() {
		`when`(rateLimits.consume("auth", "10.0.0.8", 20))
			.thenReturn(RateLimitDecision(true, 20, 19, clock.instant().epochSecond + 60))
		val request = MockHttpServletRequest("POST", "/api/auth/oauth")
		request.remoteAddr = "10.0.0.8"

		filter.doFilter(request, MockHttpServletResponse(), MockFilterChain())
	}

	@Test
	fun `login uses the auth bucket and client ip`() {
		`when`(rateLimits.consume("auth", "10.0.0.8", 20))
			.thenReturn(RateLimitDecision(true, 20, 19, clock.instant().epochSecond + 60))
		val request = MockHttpServletRequest("POST", "/api/auth/login")
		request.remoteAddr = "10.0.0.8"
		val response = MockHttpServletResponse()

		filter.doFilter(request, response, MockFilterChain())

		assertEquals("20", response.getHeader(RateLimitFilter.HEADER_LIMIT))
		assertEquals("19", response.getHeader(RateLimitFilter.HEADER_REMAINING))
		assertEquals(200, response.status)
	}

	@Test
	fun `overflow returns 429 with RATE_LIMITED`() {
		`when`(rateLimits.consume("auth", "10.0.0.8", 20))
			.thenReturn(RateLimitDecision(false, 20, 0, clock.instant().epochSecond + 60))
		val request = MockHttpServletRequest("POST", "/api/auth/login")
		request.remoteAddr = "10.0.0.8"
		val response = MockHttpServletResponse()

		filter.doFilter(request, response, MockFilterChain())

		assertEquals(429, response.status)
		assertEquals("60", response.getHeader(HttpHeaders.RETRY_AFTER))
		assertTrue(response.contentAsString.contains(ApiProblem.RATE_LIMITED), response.contentAsString)
	}

	@Test
	fun `x-forwarded-for wins over remote addr`() {
		`when`(rateLimits.consume("api", "203.0.113.9", 300))
			.thenReturn(RateLimitDecision(true, 300, 299, clock.instant().epochSecond + 60))
		val request = MockHttpServletRequest("GET", "/api/trips")
		request.remoteAddr = "10.0.0.8"
		request.addHeader("X-Forwarded-For", "203.0.113.9, 10.0.0.1")

		filter.doFilter(request, MockHttpServletResponse(), MockFilterChain())
	}
}
