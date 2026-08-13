package com.shoptourr.web

import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.ProblemDetail
import java.net.URI

/**
 * Single place where the API error contract is built, so every error — whether it comes from
 * the security filter chain, from Spring MVC, or from a controller — carries the same shape:
 * RFC 9457 `ProblemDetail` plus a stable machine-readable `code`.
 *
 * Codes are part of the public contract: once shipped, a code keeps its meaning.
 */
object ApiProblem {

	private const val TYPE_BASE = "https://api.shoptourr.com/problems/"

	const val VALIDATION_ERROR = "VALIDATION_ERROR"
	const val UNAUTHORIZED = "UNAUTHORIZED"
	const val FORBIDDEN = "FORBIDDEN"
	const val NOT_FOUND = "NOT_FOUND"
	const val CONFLICT = "CONFLICT"
	const val MEDIA_NOT_READY = "MEDIA_NOT_READY"
	const val INTERNAL_ERROR = "INTERNAL_ERROR"

	fun of(
		status: HttpStatus,
		code: String,
		title: String,
		detail: String,
	): ProblemDetail {
		val problem = ProblemDetail.forStatusAndDetail(status, detail)
		problem.title = title
		return stamp(problem, code)
	}

	/** Adds `code` and the matching `type` URI, leaving an already-stamped problem untouched. */
	fun stamp(problem: ProblemDetail, code: String): ProblemDetail {
		if (problem.properties?.get("code") == null) {
			problem.setProperty("code", code)
		}
		if (problem.type == ProblemDetail.forStatus(problem.status).type) {
			val actual = problem.properties?.get("code") as? String ?: code
			problem.type = URI.create(TYPE_BASE + actual.lowercase().replace('_', '-'))
		}
		return problem
	}

	/**
	 * Default code for a status Spring MVC handled on its own — `404` becomes `NOT_FOUND`,
	 * `415` becomes `UNSUPPORTED_MEDIA_TYPE`, and so on.
	 */
	fun defaultCode(status: HttpStatusCode): String =
		when (status.value()) {
			HttpStatus.INTERNAL_SERVER_ERROR.value() -> INTERNAL_ERROR
			else -> HttpStatus.resolve(status.value())?.name ?: "ERROR_${status.value()}"
		}
}
