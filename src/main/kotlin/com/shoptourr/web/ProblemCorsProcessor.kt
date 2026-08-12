package com.shoptourr.web

import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.server.ServerHttpResponse
import org.springframework.web.cors.DefaultCorsProcessor
import tools.jackson.databind.json.JsonMapper

/**
 * A rejected CORS request is answered by the filter itself, long before any controller, and
 * Spring's default writes a bare `text/plain` body. That was the one response left that did not
 * follow the API error contract.
 */
class ProblemCorsProcessor(
	private val jsonMapper: JsonMapper,
) : DefaultCorsProcessor() {

	override fun rejectRequest(response: ServerHttpResponse) {
		val problem = ApiProblem.of(
			HttpStatus.FORBIDDEN,
			ApiProblem.FORBIDDEN,
			"Forbidden",
			"This origin is not allowed to call the API.",
		)
		response.setStatusCode(HttpStatus.FORBIDDEN)
		response.headers.contentType = MediaType.APPLICATION_PROBLEM_JSON
		response.body.write(jsonMapper.writeValueAsBytes(problem))
		response.flush()
	}
}
