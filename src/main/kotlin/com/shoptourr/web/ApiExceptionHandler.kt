package com.shoptourr.web

import com.shoptourr.AuthenticationFailedException
import com.shoptourr.DomainValidationException
import com.shoptourr.IdempotencyConflictException
import com.shoptourr.MediaNotReadyException
import com.shoptourr.ResourceConflictException
import com.shoptourr.ResourceNotFoundException
import io.valix.spring.ValixValidationException
import jakarta.validation.ConstraintViolationException
import org.slf4j.LoggerFactory
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
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
import org.springframework.web.servlet.resource.NoResourceFoundException

/**
 * Owns the API error contract.
 *
 * Extends [ResponseEntityExceptionHandler] on purpose: Spring MVC's own exceptions (404, 405,
 * 415, unreadable body, type mismatch…) are already rendered as `ProblemDetail` by the parent,
 * and [handleExceptionInternal] stamps every one of them with a `code`. Boot's built-in
 * problem-details advice backs off in the presence of this bean, so there is exactly one place
 * deciding what an error looks like on the wire.
 *
 * The explicit [Order] is load-bearing: `valix-spring` auto-configures its own
 * `ValixControllerAdvice`, which renders a bare `{status, error, errors}` map — no `code`, no
 * `type`, and a `rejectedValue` echoing the offending input. Both advices default to
 * `LOWEST_PRECEDENCE`, so without this annotation which one claims a
 * [ValixValidationException] is left to bean ordering. Highest precedence keeps this class the
 * single owner of the contract.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
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

	/**
	 * Spring's own message is "No static resource api/orders." — an implementation detail of the
	 * resource handler that has no business leaking to an API client.
	 */
	override fun handleNoResourceFoundException(
		ex: NoResourceFoundException,
		headers: HttpHeaders,
		status: HttpStatusCode,
		request: WebRequest,
	): ResponseEntity<Any>? {
		val problem = ApiProblem.of(
			HttpStatus.NOT_FOUND,
			ApiProblem.NOT_FOUND,
			"Not found",
			"No endpoint matches this request.",
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

	/**
	 * Valix rejected a `@ValidValix` request body. Rendered exactly like the jakarta path above so
	 * a client cannot tell which validator ran — `rejectedValue` is deliberately dropped rather
	 * than forwarded, since it carries the raw input that failed.
	 */
	@ExceptionHandler(ValixValidationException::class)
	fun valixValidation(ex: ValixValidationException): ProblemDetail {
		val errors = ex.validationResult.errors
		val problem = ApiProblem.of(
			HttpStatus.BAD_REQUEST,
			ApiProblem.VALIDATION_ERROR,
			"Validation failed",
			errors.firstOrNull()?.message ?: "Validation failed",
		)
		problem.setProperty(
			"errors",
			errors.map { error ->
				mapOf(
					"field" to error.field,
					"code" to error.code,
					"message" to error.message,
				)
			},
		)
		return problem
	}

	@ExceptionHandler(DomainValidationException::class)
	fun domainValidation(ex: DomainValidationException): ProblemDetail =
		ApiProblem.of(
			HttpStatus.BAD_REQUEST,
			ApiProblem.VALIDATION_ERROR,
			"Validation failed",
			ex.message ?: "Validation failed",
		)

	@ExceptionHandler(MediaNotReadyException::class)
	fun mediaNotReady(ex: MediaNotReadyException): ProblemDetail =
		ApiProblem.of(
			HttpStatus.CONFLICT,
			ApiProblem.MEDIA_NOT_READY,
			"Media not ready",
			ex.message ?: "Media is not ready.",
		)

	@ExceptionHandler(IdempotencyConflictException::class)
	fun idempotencyConflict(ex: IdempotencyConflictException): ProblemDetail =
		ApiProblem.of(
			HttpStatus.CONFLICT,
			ApiProblem.IDEMPOTENCY_CONFLICT,
			"Idempotency conflict",
			ex.message ?: "Idempotency-Key was reused with a different request body.",
		)

	@ExceptionHandler(ResourceConflictException::class)
	fun conflict(ex: ResourceConflictException): ProblemDetail =
		ApiProblem.of(
			HttpStatus.CONFLICT,
			ApiProblem.CONFLICT,
			"Conflict",
			ex.message ?: "The request conflicts with the current state of the resource.",
		)

	@ExceptionHandler(ResourceNotFoundException::class)
	fun notFound(ex: ResourceNotFoundException): ProblemDetail =
		ApiProblem.of(
			HttpStatus.NOT_FOUND,
			ApiProblem.NOT_FOUND,
			"Not found",
			ex.message ?: "No such resource.",
		)

	@ExceptionHandler(AuthenticationFailedException::class)
	fun authenticationFailed(ex: AuthenticationFailedException): ProblemDetail =
		ApiProblem.of(
			HttpStatus.UNAUTHORIZED,
			ApiProblem.UNAUTHORIZED,
			"Unauthorized",
			ex.message ?: "Authentication is required to access this resource.",
		)

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
