package com.shoptourr.web

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ProblemDetail
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.stereotype.Component
import tools.jackson.databind.json.JsonMapper

/**
 * 401 and 403 are produced inside the security filter chain, before any controller runs, so
 * `@RestControllerAdvice` never sees them. Without these handlers those responses fall back to
 * the servlet container default and break the API error contract.
 */
@Component
class ProblemAuthenticationEntryPoint(
	private val jsonMapper: JsonMapper,
) : AuthenticationEntryPoint {

	override fun commence(
		request: HttpServletRequest,
		response: HttpServletResponse,
		authException: AuthenticationException,
	) {
		writeProblem(
			response,
			jsonMapper,
			ApiProblem.of(
				HttpStatus.UNAUTHORIZED,
				ApiProblem.UNAUTHORIZED,
				"Unauthorized",
				"Authentication is required to access this resource.",
			),
		)
	}
}

@Component
class ProblemAccessDeniedHandler(
	private val jsonMapper: JsonMapper,
) : AccessDeniedHandler {

	override fun handle(
		request: HttpServletRequest,
		response: HttpServletResponse,
		accessDeniedException: AccessDeniedException,
	) {
		writeProblem(
			response,
			jsonMapper,
			ApiProblem.of(
				HttpStatus.FORBIDDEN,
				ApiProblem.FORBIDDEN,
				"Forbidden",
				"You do not have permission to access this resource.",
			),
		)
	}
}

private fun writeProblem(
	response: HttpServletResponse,
	jsonMapper: JsonMapper,
	problem: ProblemDetail,
) {
	response.status = problem.status
	response.contentType = MediaType.APPLICATION_PROBLEM_JSON_VALUE
	response.characterEncoding = Charsets.UTF_8.name()
	response.writer.write(jsonMapper.writeValueAsString(problem))
}
