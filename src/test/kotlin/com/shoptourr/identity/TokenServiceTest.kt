package com.shoptourr.identity

import com.shoptourr.config.JwtConfig
import com.shoptourr.config.JwtProperties
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID

class TokenServiceTest {

	private val clock = Clock.fixed(Instant.parse("2026-08-13T09:00:00Z"), ZoneOffset.UTC)
	private val props = JwtProperties(
		secret = "test-only-secret-key-32bytes-min!!",
		accessTokenTtl = Duration.ofMinutes(15),
		refreshTokenTtl = Duration.ofDays(30),
	)
	private val jwt = JwtConfig(props)
	private val service = TokenService(jwt.jwtEncoder(jwt.jwtSecretKey()), props, clock)

	@Test
	fun `hash is 64-char lowercase hex and stable`() {
		val hash = service.hash("refresh-token-value")

		assertEquals(64, hash.length)
		assertEquals(hash, service.hash("refresh-token-value"))
		assertTrue(hash.matches(Regex("[0-9a-f]+")), hash)
	}

	@Test
	fun `access token carries subject email and session id`() {
		val now = Instant.parse("2099-01-01T00:00:00Z")
		val clock = Clock.fixed(now, ZoneOffset.UTC)
		val service = TokenService(jwt.jwtEncoder(jwt.jwtSecretKey()), props, clock)
		val user = AppUser(
			email = "ada@example.com",
			passwordHash = "x",
			displayName = "Ada",
			createdAt = now,
			updatedAt = now,
		)
		val sessionId = UUID.fromString("11111111-1111-1111-1111-111111111111")

		val encoded = service.issueAccessToken(user, sessionId)
		val decoded = jwt.jwtDecoder(jwt.jwtSecretKey()).decode(encoded)

		assertEquals(user.id.toString(), decoded.subject)
		assertEquals("ada@example.com", decoded.getClaimAsString("email"))
		assertEquals(sessionId.toString(), decoded.getClaimAsString(TokenService.SESSION_CLAIM))
		assertEquals(now.plus(props.accessTokenTtl), decoded.expiresAt)
	}

	@Test
	fun `new refresh tokens are unique opaque values`() {
		assertNotEquals(service.newRefreshTokenValue(), service.newRefreshTokenValue())
	}
}
