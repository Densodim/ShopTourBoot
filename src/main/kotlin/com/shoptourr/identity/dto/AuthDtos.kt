package com.shoptourr.identity.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

data class RegisterRequest(
	@field:NotBlank
	@field:Size(min = 2, max = 80)
	val displayName: String,

	@field:NotBlank
	@field:Email
	@field:Size(max = 254)
	val email: String,

	@field:NotBlank
	@field:Size(min = 6, max = 128)
	val password: String,

	@field:Size(min = 2, max = 5)
	val locale: String? = null,
)

data class LoginRequest(
	@field:NotBlank
	@field:Email
	val email: String,

	@field:NotBlank
	@field:Size(min = 1, max = 128)
	val password: String,

	@field:Size(max = 120)
	val deviceName: String? = null,
)

data class RefreshTokenRequest(
	@field:NotBlank
	val refreshToken: String,
)

data class LogoutRequest(
	val refreshToken: String? = null,
	val allSessions: Boolean = false,
)

data class ForgotPasswordRequest(
	@field:NotBlank
	@field:Email
	val email: String,
)

data class AuthUserDto(
	val id: UUID,
	val displayName: String,
	val email: String,
	val locale: String,
	val createdAt: Instant,
)

data class AuthTokensResponse(
	val accessToken: String,
	val accessExpiresIn: Long,
	val refreshToken: String,
	val refreshExpiresIn: Long,
	val tokenType: String = "Bearer",
	val user: AuthUserDto,
)
