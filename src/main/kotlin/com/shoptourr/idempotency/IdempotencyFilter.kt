package com.shoptourr.idempotency

import com.shoptourr.DomainValidationException
import com.shoptourr.IdempotencyConflictException
import com.shoptourr.web.ApiProblem
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.util.ContentCachingResponseWrapper
import tools.jackson.databind.json.JsonMapper
import java.util.UUID

class IdempotencyFilter(
	private val idempotencyService: IdempotencyService,
	private val jsonMapper: JsonMapper,
) : OncePerRequestFilter() {

	override fun shouldNotFilter(request: HttpServletRequest): Boolean {
		if (!request.method.equals("POST", ignoreCase = true)) {
			return true
		}
		if (!apiPath(request).startsWith("/api/")) {
			return true
		}
		return request.getHeader(IdempotencyService.HEADER).isNullOrBlank()
	}

	override fun doFilterInternal(
		request: HttpServletRequest,
		response: HttpServletResponse,
		filterChain: FilterChain,
	) {
		val userId = currentUserId()
		if (userId == null) {
			filterChain.doFilter(request, response)
			return
		}
		val wrappedRequest = RepeatableBodyRequest(request)
		val key = request.getHeader(IdempotencyService.HEADER).orEmpty()
		val routeKey = "${request.method.uppercase()} ${apiPath(request)}"
		val requestHash = IdempotencyService.hashBody(wrappedRequest.body)
		val replay = try {
			idempotencyService.replayOrNull(userId, routeKey, key, requestHash)
		} catch (ex: IdempotencyConflictException) {
			writeConflict(response)
			return
		} catch (ex: DomainValidationException) {
			writeValidation(response, ex.message ?: "Validation failed")
			return
		}
		if (replay != null) {
			writeReplay(response, replay)
			return
		}
		val wrappedResponse = ContentCachingResponseWrapper(response)
		try {
			filterChain.doFilter(wrappedRequest, wrappedResponse)
			val body = wrappedResponse.contentAsByteArray.toString(Charsets.UTF_8)
			idempotencyService.remember(userId, routeKey, key, requestHash, wrappedResponse.status, body)
		} finally {
			wrappedResponse.copyBodyToResponse()
		}
	}

	private fun currentUserId(): UUID? {
		val principal = SecurityContextHolder.getContext().authentication?.principal as? Jwt ?: return null
		return runCatching { UUID.fromString(principal.subject) }.getOrNull()
	}

	private fun writeReplay(response: HttpServletResponse, replay: IdempotentReplay) {
		response.status = replay.status
		response.contentType = MediaType.APPLICATION_JSON_VALUE
		response.characterEncoding = Charsets.UTF_8.name()
		response.outputStream.write(replay.body.toByteArray(Charsets.UTF_8))
	}

	private fun writeConflict(response: HttpServletResponse) {
		writeProblem(
			response,
			ApiProblem.of(
				HttpStatus.CONFLICT,
				ApiProblem.IDEMPOTENCY_CONFLICT,
				"Idempotency conflict",
				"Idempotency-Key was reused with a different request body.",
			),
		)
	}

	private fun writeValidation(response: HttpServletResponse, detail: String) {
		writeProblem(
			response,
			ApiProblem.of(
				HttpStatus.BAD_REQUEST,
				ApiProblem.VALIDATION_ERROR,
				"Validation failed",
				detail,
			),
		)
	}

	private fun writeProblem(response: HttpServletResponse, problem: org.springframework.http.ProblemDetail) {
		response.status = problem.status
		response.contentType = MediaType.APPLICATION_PROBLEM_JSON_VALUE
		response.characterEncoding = Charsets.UTF_8.name()
		response.writer.write(jsonMapper.writeValueAsString(problem))
	}

	companion object {
		fun apiPath(request: HttpServletRequest): String {
			val path = request.servletPath.orEmpty().ifBlank { request.requestURI.orEmpty() }
			return path.substringBefore('?')
		}
	}
}
