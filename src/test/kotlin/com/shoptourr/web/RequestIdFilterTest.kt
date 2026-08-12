package com.shoptourr.web

import jakarta.servlet.FilterChain
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.slf4j.MDC
import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import java.util.UUID

class RequestIdFilterTest {

	private val filter = RequestIdFilter()

	private fun request(requestId: String? = null) =
		MockHttpServletRequest("GET", "/api/_ping").apply {
			requestId?.let { addHeader(RequestIdFilter.HEADER, it) }
		}

	@Test
	fun `generates a request id when the caller sends none`() {
		val response = MockHttpServletResponse()

		filter.doFilter(request(), response, MockFilterChain())

		val generated = response.getHeader(RequestIdFilter.HEADER)
		assertNotNull(generated)
		UUID.fromString(generated)
	}

	@Test
	fun `reuses the caller's request id`() {
		val response = MockHttpServletResponse()

		filter.doFilter(request("upstream-id"), response, MockFilterChain())

		assertEquals("upstream-id", response.getHeader(RequestIdFilter.HEADER))
	}

	@Test
	fun `ignores a blank request id and generates one instead`() {
		val response = MockHttpServletResponse()

		filter.doFilter(request("   "), response, MockFilterChain())

		val generated = response.getHeader(RequestIdFilter.HEADER)
		assertNotNull(generated)
		UUID.fromString(generated)
	}

	@Test
	fun `exposes the request id through the MDC while the chain runs`() {
		var seenInsideChain: String? = null
		val chain = FilterChain { _, _ -> seenInsideChain = MDC.get(RequestIdFilter.MDC_KEY) }

		filter.doFilter(request("mdc-id"), MockHttpServletResponse(), chain)

		assertEquals("mdc-id", seenInsideChain)
	}

	@Test
	fun `clears the MDC even when the chain throws`() {
		val chain = FilterChain { _, _ -> throw IllegalStateException("boom") }

		runCatching { filter.doFilter(request("leak-check"), MockHttpServletResponse(), chain) }

		assertNull(MDC.get(RequestIdFilter.MDC_KEY))
	}
}
