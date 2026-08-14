package com.shoptourr.identity

import com.shoptourr.AuthenticationFailedException
import com.shoptourr.DomainValidationException
import com.shoptourr.ResourceConflictException
import com.shoptourr.config.AuthProperties
import com.shoptourr.config.MailProperties
import com.shoptourr.identity.dto.AuthTokensResponse
import com.shoptourr.identity.dto.AuthUserDto
import com.shoptourr.identity.dto.ForgotPasswordRequest
import com.shoptourr.identity.dto.LoginRequest
import com.shoptourr.identity.dto.LogoutRequest
import com.shoptourr.identity.dto.RefreshTokenRequest
import com.shoptourr.identity.dto.RegisterRequest
import com.shoptourr.identity.dto.ResetPasswordRequest
import org.slf4j.LoggerFactory
import org.springframework.mail.MailException
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Instant
import java.util.UUID

@Service
class IdentityService(
	private val users: AppUserRepository,
	private val refreshTokens: RefreshTokenRepository,
	private val resetTokens: PasswordResetTokenRepository,
	private val passwordEncoder: PasswordEncoder,
	private val tokenService: TokenService,
	private val mailSender: JavaMailSender,
	private val mailProperties: MailProperties,
	private val authProperties: AuthProperties,
	private val clock: Clock,
) {

	private val log = LoggerFactory.getLogger(javaClass)

	@Transactional
	fun register(request: RegisterRequest): AuthTokensResponse {
		val email = normalizeEmail(request.email)
		if (users.findByEmailIgnoreCaseAndDeletedAtIsNull(email) != null) {
			throw ResourceConflictException("An account with this email already exists.")
		}
		val now = Instant.now(clock)
		val passwordHash = requireNotNull(passwordEncoder.encode(request.password)) {
			"Password encoder returned null"
		}
		val user = users.save(
			AppUser(
				email = email,
				passwordHash = passwordHash,
				displayName = request.displayName.trim(),
				locale = request.locale?.trim()?.takeIf { it.isNotBlank() } ?: DEFAULT_LOCALE,
				createdAt = now,
				updatedAt = now,
			),
		)
		return issueSession(user, deviceName = null)
	}

	@Transactional
	fun login(request: LoginRequest): AuthTokensResponse {
		val user = users.findByEmailIgnoreCaseAndDeletedAtIsNull(normalizeEmail(request.email))
		if (user == null || !passwordEncoder.matches(request.password, user.passwordHash)) {
			throw AuthenticationFailedException(INVALID_CREDENTIALS)
		}
		return issueSession(user, request.deviceName?.trim()?.takeIf { it.isNotBlank() })
	}

	@Transactional
	fun refresh(request: RefreshTokenRequest): AuthTokensResponse {
		val now = Instant.now(clock)
		val existing = refreshTokens.findByTokenHash(tokenService.hash(request.refreshToken))
		if (existing == null || !existing.isActive(now)) {
			throw AuthenticationFailedException(INVALID_REFRESH)
		}
		val user = users.findById(existing.userId).orElse(null)
			?.takeIf { it.deletedAt == null }
			?: throw AuthenticationFailedException(INVALID_REFRESH)
		existing.revokedAt = now
		return issueSession(user, existing.deviceName)
	}

	@Transactional
	fun logout(userId: UUID, sessionId: UUID?, request: LogoutRequest?) {
		val now = Instant.now(clock)
		val refreshValue = request?.refreshToken
		when {
			request?.allSessions == true ->
				refreshTokens.findAllByUserIdAndRevokedAtIsNull(userId).forEach { it.revokedAt = now }
			!refreshValue.isNullOrBlank() -> {
				val token = refreshTokens.findByTokenHash(tokenService.hash(refreshValue))
				if (token != null && token.userId == userId && token.revokedAt == null) {
					token.revokedAt = now
				}
			}
			sessionId != null ->
				refreshTokens.findById(sessionId).orElse(null)
					?.takeIf { it.userId == userId && it.revokedAt == null }
					?.let { it.revokedAt = now }
		}
	}

	@Transactional
	fun forgotPassword(request: ForgotPasswordRequest) {
		val user = users.findByEmailIgnoreCaseAndDeletedAtIsNull(normalizeEmail(request.email)) ?: return
		val now = Instant.now(clock)
		resetTokens.findAllByUserIdAndUsedAtIsNull(user.id).forEach { it.usedAt = now }
		val token = tokenService.newRefreshTokenValue()
		resetTokens.save(
			PasswordResetToken(
				userId = user.id,
				tokenHash = tokenService.hash(token),
				expiresAt = now.plus(authProperties.resetTokenTtl),
				createdAt = now,
			),
		)
		val minutes = authProperties.resetTokenTtl.toMinutes().coerceAtLeast(1)
		val message = SimpleMailMessage().apply {
			setFrom(mailProperties.from)
			setTo(user.email)
			subject = "Password reset"
			text = "We received a request to reset the password for this ShopTourr account.\n" +
				"Reset token: $token\n" +
				"It expires in $minutes minutes. If you did not ask for this, you can ignore the email."
		}
		try {
			mailSender.send(message)
		} catch (ex: MailException) {
			log.warn("Failed to send password-reset email for userId={}", user.id, ex)
		}
	}

	@Transactional
	fun resetPassword(request: ResetPasswordRequest) {
		val now = Instant.now(clock)
		val user = users.findByEmailIgnoreCaseAndDeletedAtIsNull(normalizeEmail(request.email))
		val stored = resetTokens.findByTokenHash(tokenService.hash(request.token))
		if (user == null || stored == null || stored.userId != user.id || !stored.isUsable(now)) {
			throw DomainValidationException(INVALID_RESET)
		}
		user.passwordHash = requireNotNull(passwordEncoder.encode(request.newPassword)) {
			"Password encoder returned null"
		}
		user.updatedAt = now
		stored.usedAt = now
		refreshTokens.findAllByUserIdAndRevokedAtIsNull(user.id).forEach { it.revokedAt = now }
	}

	private fun issueSession(user: AppUser, deviceName: String?): AuthTokensResponse {
		val now = Instant.now(clock)
		val refreshValue = tokenService.newRefreshTokenValue()
		val session = refreshTokens.save(
			RefreshToken(
				userId = user.id,
				tokenHash = tokenService.hash(refreshValue),
				deviceName = deviceName,
				expiresAt = tokenService.refreshExpiresAt(now),
				createdAt = now,
			),
		)
		return AuthTokensResponse(
			accessToken = tokenService.issueAccessToken(user, session.id),
			accessExpiresIn = tokenService.accessExpiresInSeconds(),
			refreshToken = refreshValue,
			refreshExpiresIn = tokenService.refreshExpiresInSeconds(),
			user = user.toAuthDto(),
		)
	}

	companion object {
		const val DEFAULT_LOCALE = "ru"
		const val INVALID_CREDENTIALS = "Invalid email or password."
		const val INVALID_REFRESH = "Refresh token is invalid or expired."
		const val INVALID_RESET = "Invalid or expired reset token."

		fun normalizeEmail(email: String): String = email.trim().lowercase()
	}
}

fun AppUser.toAuthDto(): AuthUserDto =
	AuthUserDto(
		id = id,
		displayName = displayName,
		email = email,
		locale = locale,
		createdAt = createdAt,
	)
