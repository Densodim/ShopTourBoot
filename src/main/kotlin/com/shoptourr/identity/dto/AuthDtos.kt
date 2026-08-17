package com.shoptourr.identity.dto

import io.valix.annotations.Email
import io.valix.annotations.MaxLength
import io.valix.annotations.MinLength
import io.valix.annotations.NotBlank
import io.valix.annotations.Sensitive
import java.time.Instant
import java.util.UUID

/**
 * Auth request bodies are validated by Valix: KSP generates a `…Validator` per class at build
 * time and the controller opts in with `@ValidValix`. Every secret carries [Sensitive] so the
 * generated error never repeats a raw password or token back to the caller.
 *
 * Valix has no string-aware `@Size`, so a jakarta `@Size(min, max)` on a String becomes the
 * [MinLength]/[MaxLength] pair; nullable properties are null-guarded by the generated code, which
 * matches jakarta's "absent means valid" semantics.
 */
data class RegisterRequest(
	@NotBlank
	@MinLength(2)
	@MaxLength(80)
	val displayName: String,

	@NotBlank
	@Email
	@MaxLength(254)
	val email: String,

	@NotBlank
	@MinLength(6)
	@MaxLength(128)
	@Sensitive
	val password: String,

	@MinLength(2)
	@MaxLength(5)
	val locale: String? = null,
)

data class LoginRequest(
	@NotBlank
	@Email
	val email: String,

	@NotBlank
	@MaxLength(128)
	@Sensitive
	val password: String,

	@MaxLength(120)
	val deviceName: String? = null,
)

data class RefreshTokenRequest(
	@NotBlank
	@Sensitive
	val refreshToken: String,
)

data class LogoutRequest(
	val refreshToken: String? = null,
	val allSessions: Boolean = false,
)

data class ForgotPasswordRequest(
	@NotBlank
	@Email
	val email: String,
)

data class ResetPasswordRequest(
	@NotBlank
	@Email
	val email: String,

	@NotBlank
	@MinLength(16)
	@MaxLength(128)
	@Sensitive
	val token: String,

	@NotBlank
	@MinLength(6)
	@MaxLength(128)
	@Sensitive
	val newPassword: String,
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
