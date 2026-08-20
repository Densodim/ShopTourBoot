package com.shoptourr.ratelimit

import com.shoptourr.config.RateLimitProperties
import com.shoptourr.web.ApiProblem
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.filter.OncePerRequestFilter
import tools.jackson.databind.json.JsonMapper
import java.time.Clock

class RateLimitFilter(
	private val rateLimits: RateLimitService,
	private val properties: RateLimitProperties,
	private val jsonMapper: JsonMapper,
	private val clock: Clock,
) : OncePerRequestFilter() {

	override fun shouldNotFilter(request: HttpServletRequest): Boolean {
		if (!properties.enabled) {
			return true
		}
		if (request.method.equals("OPTIONS", ignoreCase = true)) {
			return true
		}
		val path = apiPath(request)
		if (path == "/api/_ping") {
			return true
		}
		return !path.startsWith("/api/") &&
			!path.startsWith("/dev-uploads/") &&
			!path.startsWith("/dev-exports/")
	}

	override fun doFilterInternal(
		request: HttpServletRequest,
		response: HttpServletResponse,
		filterChain: FilterChain,
	) {
		val path = apiPath(request)
		val authSensitive = AUTH_PATHS.contains(path)
		val bucket = if (authSensitive) "auth" else "api"
		val limit = if (authSensitive) properties.authPerWindow else properties.apiPerWindow
		val decision = rateLimits.consume(bucket, clientId(request), limit)
		response.setHeader(HEADER_LIMIT, decision.limit.toString())
		response.setHeader(HEADER_REMAINING, decision.remaining.toString())
		response.setHeader(HEADER_RESET, decision.resetEpochSeconds.toString())
		if (!decision.allowed) {
			val retryAfter = (decision.resetEpochSeconds - clock.instant().epochSecond).coerceAtLeast(1)
			response.setHeader(HttpHeaders.RETRY_AFTER, retryAfter.toString())
			writeLimited(response)
			return
		}
		filterChain.doFilter(request, response)
	}

	private fun writeLimited(response: HttpServletResponse) {
		val problem = ApiProblem.of(
			HttpStatus.TOO_MANY_REQUESTS,
			ApiProblem.RATE_LIMITED,
			"Too many requests",
			"Slow down and retry after the window resets.",
		)
		response.status = problem.status
		response.contentType = MediaType.APPLICATION_PROBLEM_JSON_VALUE
		response.characterEncoding = Charsets.UTF_8.name()
		response.writer.write(jsonMapper.writeValueAsString(problem))
	}

	companion object {
		const val HEADER_LIMIT = "X-RateLimit-Limit"
		const val HEADER_REMAINING = "X-RateLimit-Remaining"
		const val HEADER_RESET = "X-RateLimit-Reset"

		private val AUTH_PATHS = setOf(
			"/api/auth/login",
			"/api/auth/oauth",
			"/api/auth/register",
			"/api/auth/forgot-password",
			"/api/auth/reset-password",
			"/api/auth/refresh",
		)

		fun apiPath(request: HttpServletRequest): String {
			val path = request.servletPath.orEmpty().ifBlank { request.requestURI.orEmpty() }
			return path.substringBefore('?')
		}

		fun clientId(request: HttpServletRequest): String {
			val forwarded = request.getHeader("X-Forwarded-For")?.substringBefore(',')?.trim().orEmpty()
			return forwarded.ifBlank { request.remoteAddr.orEmpty().ifBlank { "unknown" } }
		}
	}
}
