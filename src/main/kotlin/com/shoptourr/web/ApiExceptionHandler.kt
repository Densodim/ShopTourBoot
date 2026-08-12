package com.shoptourr.web

import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.net.URI

@RestControllerAdvice
class ApiExceptionHandler {

	@ExceptionHandler(MethodArgumentNotValidException::class)
	fun validation(ex: MethodArgumentNotValidException): ProblemDetail {
		val detail = ProblemDetail.forStatusAndDetail(
			HttpStatus.BAD_REQUEST,
			ex.bindingResult.fieldErrors.firstOrNull()?.defaultMessage ?: "Validation failed",
		)
		detail.title = "Validation failed"
		detail.type = URI.create("https://api.shoptourr.com/problems/validation")
		detail.setProperty("code", "VALIDATION_ERROR")
		detail.setProperty(
			"errors",
			ex.bindingResult.fieldErrors.map { err ->
				mapOf(
					"field" to (err.field),
					"code" to (err.code ?: "INVALID"),
					"message" to (err.defaultMessage ?: "invalid"),
				)
			},
		)
		return detail
	}
}
