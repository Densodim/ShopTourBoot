package com.shoptourr.config

/**
 * The single source of truth for what is reachable without authentication.
 *
 * [SecurityConfig] permits these, and [OpenApiConfig] uses the same list to decide which
 * operations should *not* be documented as returning 401/403. Adding a path here makes it
 * public — treat it as a security review, not a formality.
 */
object PublicEndpoints {

	val API: List<String> = listOf(
		"/api/auth/register",
		"/api/auth/login",
		"/api/auth/refresh",
		"/api/auth/forgot-password",
		"/api/_ping",
		"/dev-uploads/**",
	)

	/** Documentation endpoints. Switched off entirely in prod — see `application-prod.yml`. */
	val DOCS: List<String> = listOf(
		"/v3/api-docs/**",
		"/swagger-ui/**",
		"/swagger-ui.html",
	)
}
