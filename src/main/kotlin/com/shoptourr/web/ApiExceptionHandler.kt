package com.shoptourr.web

import jakarta.validation.ConstraintViolationException
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler

/**
 * Owns the API error contract.
 *
 * Extends [ResponseEntityExceptionHandler] on purpose: Spring MVC's own exceptions (404, 405,
 * 415, unreadable body, type mismatch…) are already rendered as `ProblemDetail` by the parent,
 * and [handleExceptionInternal] stamps every one of them with a `code`. Boot's built-in
 * problem-details advice backs off in the presence of this bean, so there is exactly one place
 * deciding what an error looks like on the wire.
 */
@RestControllerAdvice
class ApiExceptionHandler : ResponseEntityExceptionHandler() {

	private val log = LoggerFactory.getLogger(javaClass)

	override fun handleMethodArgumentNotValid(
		ex: MethodArgumentNotValidException,
		headers: HttpHeaders,
		status: HttpStatusCode,
		request: WebRequest,
	): ResponseEntity<Any>? {
		val problem = ApiProblem.of(
			HttpStatus.BAD_REQUEST,
			ApiProblem.VALIDATION_ERROR,
			"Validation failed",
			ex.bindingResult.fieldErrors.firstOrNull()?.defaultMessage ?: "Validation failed",
		)
		problem.setProperty(
			"errors",
			ex.bindingResult.fieldErrors.map { err ->
				mapOf(
					"field" to (err.field),
					"code" to (err.code ?: "INVALID"),
					"message" to (err.defaultMessage ?: "invalid"),
				)
			},
		)
		return handleExceptionInternal(ex, problem, headers, status, request)
	}

	/** `@Validated` on request params and path variables, as opposed to `@Valid` on a body. */
	@ExceptionHandler(ConstraintViolationException::class)
	fun constraintViolation(ex: ConstraintViolationException): ProblemDetail {
		val problem = ApiProblem.of(
			HttpStatus.BAD_REQUEST,
			ApiProblem.VALIDATION_ERROR,
			"Validation failed",
			ex.constraintViolations.firstOrNull()?.message ?: "Validation failed",
		)
		problem.setProperty(
			"errors",
			ex.constraintViolations.map { violation ->
				mapOf(
					"field" to violation.propertyPath.toString(),
					"code" to "INVALID",
					"message" to violation.message,
				)
			},
		)
		return problem
	}

	/** Thrown by method security (`@PreAuthorize`); filter-chain denials go through [ProblemAccessDeniedHandler]. */
	@ExceptionHandler(AccessDeniedException::class)
	fun accessDenied(ex: AccessDeniedException): ProblemDetail =
		ApiProblem.of(
			HttpStatus.FORBIDDEN,
			ApiProblem.FORBIDDEN,
			"Forbidden",
			"You do not have permission to access this resource.",
		)

	@ExceptionHandler(DataIntegrityViolationException::class)
	fun conflict(ex: DataIntegrityViolationException): ProblemDetail {
		log.warn("Data integrity violation", ex)
		return ApiProblem.of(
			HttpStatus.CONFLICT,
			ApiProblem.CONFLICT,
			"Conflict",
			"The request conflicts with the current state of the resource.",
		)
	}

	/**
	 * Last resort for exceptions no more specific handler claimed. The cause is logged together
	 * with the request id from the MDC; the client only ever sees a generic message, matching
	 * `server.error.include-message: never`.
	 */
	@ExceptionHandler(Exception::class)
	fun unexpected(ex: Exception): ProblemDetail {
		log.error("Unhandled exception", ex)
		return ApiProblem.of(
			HttpStatus.INTERNAL_SERVER_ERROR,
			ApiProblem.INTERNAL_ERROR,
			"Internal server error",
			"Something went wrong. If it keeps happening, quote the X-Request-Id header.",
		)
	}

	/** Every response funnelled through the parent gets a `code`, including Spring's own. */
	override fun handleExceptionInternal(
		ex: Exception,
		body: Any?,
		headers: HttpHeaders,
		statusCode: HttpStatusCode,
		request: WebRequest,
	): ResponseEntity<Any>? {
		val response = super.handleExceptionInternal(ex, body, headers, statusCode, request)
		(response?.body as? ProblemDetail)?.let { ApiProblem.stamp(it, ApiProblem.defaultCode(statusCode)) }
		return response
	}
}
