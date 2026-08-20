package com.shoptourr.identity

import com.shoptourr.AuthenticationFailedException
import com.shoptourr.DomainValidationException
import com.shoptourr.ResourceConflictException
import com.shoptourr.config.AuthProperties
import com.shoptourr.config.JwtConfig
import com.shoptourr.config.JwtProperties
import com.shoptourr.config.MailProperties
import com.shoptourr.identity.dto.ForgotPasswordRequest
import com.shoptourr.identity.dto.LoginRequest
import com.shoptourr.identity.dto.LogoutRequest
import com.shoptourr.identity.dto.RefreshTokenRequest
import com.shoptourr.identity.dto.RegisterRequest
import com.shoptourr.identity.dto.ResetPasswordRequest
import com.shoptourr.identity.dto.SocialLoginRequest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.any
import org.mockito.Mock
import org.mockito.Mockito.lenient
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.util.Optional
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class IdentityServiceTest {

	@Mock
	private lateinit var users: AppUserRepository

	@Mock
	private lateinit var refreshTokens: RefreshTokenRepository

	@Mock
	private lateinit var resetTokens: PasswordResetTokenRepository

	@Mock
	private lateinit var mailSender: JavaMailSender

	@Mock
	private lateinit var socialTokens: SocialTokenVerifier

	private val clock = Clock.fixed(Instant.parse("2026-08-13T09:00:00Z"), ZoneOffset.UTC)
	private val encoder = BCryptPasswordEncoder()
	private val jwtProps = JwtProperties(
		secret = "test-only-secret-key-32bytes-min!!",
		accessTokenTtl = Duration.ofMinutes(15),
		refreshTokenTtl = Duration.ofDays(30),
	)
	private lateinit var tokenService: TokenService
	private lateinit var service: IdentityService

	@BeforeEach
	fun setUp() {
		val jwt = JwtConfig(jwtProps)
		tokenService = TokenService(jwt.jwtEncoder(jwt.jwtSecretKey()), jwtProps, clock)
		service = IdentityService(
			users,
			refreshTokens,
			resetTokens,
			encoder,
			tokenService,
			mailSender,
			MailProperties(from = "noreply@test.example"),
			AuthProperties(),
			socialTokens,
			clock,
		)
		lenient().`when`(refreshTokens.save(any(RefreshToken::class.java))).thenAnswer { it.arguments[0] }
		lenient().`when`(resetTokens.save(any(PasswordResetToken::class.java))).thenAnswer { it.arguments[0] }
		lenient().`when`(resetTokens.findAllByUserIdAndUsedAtIsNull(any() ?: UUID.randomUUID())).thenReturn(emptyList())
	}

	@Test
	fun `register stores a hashed password and returns tokens`() {
		`when`(users.findByEmailIgnoreCaseAndDeletedAtIsNull("ada@example.com")).thenReturn(null)
		`when`(users.save(any(AppUser::class.java))).thenAnswer { it.arguments[0] }

		val result = service.register(RegisterRequest("Ada Lovelace", "Ada@Example.com", "secret1", "en"))

		assertEquals("Ada Lovelace", result.user.displayName)
		assertEquals("ada@example.com", result.user.email)
		assertEquals("en", result.user.locale)
		assertEquals("Bearer", result.tokenType)
		assertEquals(900, result.accessExpiresIn)
		assertTrue(result.accessToken.isNotBlank())
		assertTrue(result.refreshToken.isNotBlank())
		verify(users).save(any(AppUser::class.java))
	}

	@Test
	fun `register defaults locale to ru`() {
		`when`(users.findByEmailIgnoreCaseAndDeletedAtIsNull("ada@example.com")).thenReturn(null)
		`when`(users.save(any(AppUser::class.java))).thenAnswer { it.arguments[0] }

		val result = service.register(RegisterRequest("Ada", "ada@example.com", "secret1"))

		assertEquals("ru", result.user.locale)
	}

	@Test
	fun `register rejects a live email`() {
		`when`(users.findByEmailIgnoreCaseAndDeletedAtIsNull("ada@example.com")).thenReturn(existingUser())

		assertThrows<ResourceConflictException> {
			service.register(RegisterRequest("Ada", "ada@example.com", "secret1"))
		}
	}

	@Test
	fun `login returns tokens for a matching password`() {
		val user = existingUser()
		`when`(users.findByEmailIgnoreCaseAndDeletedAtIsNull("ada@example.com")).thenReturn(user)

		val result = service.login(LoginRequest("ada@example.com", "secret1", "iPhone"))

		assertEquals(user.id, result.user.id)
		assertTrue(result.accessToken.isNotBlank())
	}

	@Test
	fun `login rejects a wrong password with the same message as an unknown email`() {
		`when`(users.findByEmailIgnoreCaseAndDeletedAtIsNull("ada@example.com")).thenReturn(existingUser())
		`when`(users.findByEmailIgnoreCaseAndDeletedAtIsNull("missing@example.com")).thenReturn(null)

		val wrong = assertThrows<AuthenticationFailedException> {
			service.login(LoginRequest("ada@example.com", "nope!!", null))
		}
		val missing = assertThrows<AuthenticationFailedException> {
			service.login(LoginRequest("missing@example.com", "secret1", null))
		}

		assertEquals(IdentityService.INVALID_CREDENTIALS, wrong.message)
		assertEquals(wrong.message, missing.message)
	}

	@Test
	fun `refresh rotates the token and rejects the old one`() {
		val user = existingUser()
		val oldValue = "old-refresh-token"
		val stored = RefreshToken(
			userId = user.id,
			tokenHash = tokenService.hash(oldValue),
			expiresAt = Instant.now(clock).plus(Duration.ofDays(1)),
			createdAt = Instant.now(clock),
		)
		`when`(refreshTokens.findByTokenHash(tokenService.hash(oldValue))).thenReturn(stored)
		`when`(users.findById(user.id)).thenReturn(Optional.of(user))

		val result = service.refresh(RefreshTokenRequest(oldValue))

		assertNotEquals(oldValue, result.refreshToken)
		assertNotNull(stored.revokedAt)
		assertEquals(Instant.now(clock), stored.revokedAt)
	}

	@Test
	fun `refresh rejects a revoked token`() {
		val stored = RefreshToken(
			userId = UUID.randomUUID(),
			tokenHash = tokenService.hash("revoked"),
			expiresAt = Instant.now(clock).plus(Duration.ofDays(1)),
			revokedAt = Instant.now(clock),
			createdAt = Instant.now(clock),
		)
		`when`(refreshTokens.findByTokenHash(tokenService.hash("revoked"))).thenReturn(stored)

		val ex = assertThrows<AuthenticationFailedException> {
			service.refresh(RefreshTokenRequest("revoked"))
		}
		assertEquals(IdentityService.INVALID_REFRESH, ex.message)
	}

	@Test
	fun `logout all sessions revokes every live refresh token`() {
		val user = existingUser()
		val live = listOf(
			RefreshToken(userId = user.id, tokenHash = "a", expiresAt = Instant.now(clock).plusSeconds(60), createdAt = Instant.now(clock)),
			RefreshToken(userId = user.id, tokenHash = "b", expiresAt = Instant.now(clock).plusSeconds(60), createdAt = Instant.now(clock)),
		)
		`when`(refreshTokens.findAllByUserIdAndRevokedAtIsNull(user.id)).thenReturn(live)

		service.logout(user.id, null, LogoutRequest(allSessions = true))

		assertTrue(live.all { it.revokedAt == Instant.now(clock) })
	}

	@Test
	fun `forgot password sends mail only when the account exists`() {
		`when`(users.findByEmailIgnoreCaseAndDeletedAtIsNull("ada@example.com")).thenReturn(existingUser())
		`when`(users.findByEmailIgnoreCaseAndDeletedAtIsNull("missing@example.com")).thenReturn(null)

		service.forgotPassword(ForgotPasswordRequest("ada@example.com"))
		service.forgotPassword(ForgotPasswordRequest("missing@example.com"))

		val captor = org.mockito.ArgumentCaptor.forClass(SimpleMailMessage::class.java)
		verify(mailSender).send(captor.capture())
		assertTrue(captor.value.text.orEmpty().contains("Reset token:"))
	}

	@Test
	fun `forgot password does not throw when mail delivery fails`() {
		`when`(users.findByEmailIgnoreCaseAndDeletedAtIsNull("ada@example.com")).thenReturn(existingUser())
		`when`(mailSender.send(any(SimpleMailMessage::class.java))).thenThrow(
			org.springframework.mail.MailSendException("down"),
		)

		service.forgotPassword(ForgotPasswordRequest("ada@example.com"))
	}

	@Test
	fun `reset password accepts a live token and revokes sessions`() {
		val user = existingUser()
		val raw = "reset-token-value-ok-32ch"
		val stored = PasswordResetToken(
			userId = user.id,
			tokenHash = tokenService.hash(raw),
			expiresAt = Instant.now(clock).plus(Duration.ofMinutes(30)),
			createdAt = Instant.now(clock),
		)
		val session = RefreshToken(
			userId = user.id,
			tokenHash = "live",
			expiresAt = Instant.now(clock).plus(Duration.ofDays(1)),
			createdAt = Instant.now(clock),
		)
		`when`(users.findByEmailIgnoreCaseAndDeletedAtIsNull("ada@example.com")).thenReturn(user)
		`when`(resetTokens.findByTokenHash(tokenService.hash(raw))).thenReturn(stored)
		`when`(refreshTokens.findAllByUserIdAndRevokedAtIsNull(user.id)).thenReturn(listOf(session))

		service.resetPassword(ResetPasswordRequest("ada@example.com", raw, "newsecret"))

		assertTrue(encoder.matches("newsecret", requireNotNull(user.passwordHash)))
		assertEquals(Instant.now(clock), stored.usedAt)
		assertEquals(Instant.now(clock), session.revokedAt)
	}

	@Test
	fun `google login creates a user without a password`() {
		val identity = SocialIdentity(
			provider = SocialProvider.GOOGLE,
			subject = "google-sub-1",
			email = "ada@example.com",
			emailVerified = true,
			displayName = "Ada",
		)
		`when`(socialTokens.verify(SocialProvider.GOOGLE, "id-token", "nonce")).thenReturn(identity)
		`when`(users.findByGoogleSubAndDeletedAtIsNull("google-sub-1")).thenReturn(null)
		`when`(users.findByEmailIgnoreCaseAndDeletedAtIsNull("ada@example.com")).thenReturn(null)
		`when`(users.save(any(AppUser::class.java))).thenAnswer { it.arguments[0] }

		val result = service.loginSocial(
			SocialLoginRequest(SocialProvider.GOOGLE, "id-token", "nonce", deviceName = "Pixel"),
		)

		assertEquals("ada@example.com", result.user.email)
		assertTrue(result.accessToken.isNotBlank())
	}

	@Test
	fun `google login links a verified email to an existing password account`() {
		val user = existingUser()
		val identity = SocialIdentity(
			provider = SocialProvider.GOOGLE,
			subject = "google-sub-1",
			email = "ada@example.com",
			emailVerified = true,
			displayName = "Ada",
		)
		`when`(socialTokens.verify(SocialProvider.GOOGLE, "id-token", null)).thenReturn(identity)
		`when`(users.findByGoogleSubAndDeletedAtIsNull("google-sub-1")).thenReturn(null)
		`when`(users.findByEmailIgnoreCaseAndDeletedAtIsNull("ada@example.com")).thenReturn(user)

		service.loginSocial(SocialLoginRequest(SocialProvider.GOOGLE, "id-token"))

		assertEquals("google-sub-1", user.googleSub)
	}

	@Test
	fun `password login is rejected for a social-only account`() {
		val user = existingUser().apply { passwordHash = null }
		`when`(users.findByEmailIgnoreCaseAndDeletedAtIsNull("ada@example.com")).thenReturn(user)

		val ex = assertThrows<AuthenticationFailedException> {
			service.login(LoginRequest("ada@example.com", "secret1"))
		}
		assertEquals(IdentityService.INVALID_CREDENTIALS, ex.message)
	}

	@Test
	fun `reset password rejects an unknown token without enumerating the user`() {
		`when`(users.findByEmailIgnoreCaseAndDeletedAtIsNull("ada@example.com")).thenReturn(existingUser())
		`when`(resetTokens.findByTokenHash(tokenService.hash("unknown-reset-token!!"))).thenReturn(null)

		val ex = assertThrows<DomainValidationException> {
			service.resetPassword(ResetPasswordRequest("ada@example.com", "unknown-reset-token!!", "newsecret"))
		}
		assertEquals(IdentityService.INVALID_RESET, ex.message)
	}

	private fun existingUser(): AppUser =
		AppUser(
			email = "ada@example.com",
			passwordHash = requireNotNull(encoder.encode("secret1")),
			displayName = "Ada",
			locale = "en",
			createdAt = Instant.now(clock),
			updatedAt = Instant.now(clock),
		)
}
