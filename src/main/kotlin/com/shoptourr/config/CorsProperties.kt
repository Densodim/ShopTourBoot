package com.shoptourr.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

/**
 * Browser origins allowed to call the API.
 *
 * Defaults cover local front-end dev servers only; deployed environments override
 * `voyage.cors.allowed-origins` (env `VOYAGE_CORS_ORIGINS`). Wildcard origins are not
 * supported here on purpose — credentials are allowed, and `*` with credentials is invalid.
 */
@ConfigurationProperties(prefix = "voyage.cors")
data class CorsProperties(
	val allowedOrigins: List<String> = listOf("http://localhost:5173", "http://localhost:3000"),
	val allowedMethods: List<String> = listOf("GET", "HEAD", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"),
	val allowedHeaders: List<String> = listOf("*"),
	val exposedHeaders: List<String> = listOf(
		"X-Request-Id",
		"Retry-After",
		"X-RateLimit-Limit",
		"X-RateLimit-Remaining",
		"X-RateLimit-Reset",
		"Upload-Offset",
		"Tus-Resumable",
	),
	val allowCredentials: Boolean = true,
	val maxAge: Duration = Duration.ofHours(1),
)
