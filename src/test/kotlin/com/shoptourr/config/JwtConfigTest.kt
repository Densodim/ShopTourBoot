package com.shoptourr.config

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.JwtEncoderParameters
import org.springframework.security.oauth2.jwt.JwsHeader
import java.time.Duration
import java.time.Instant

class JwtConfigTest {

	private val props = JwtProperties(
		secret = "test-only-secret-key-32bytes-min!!",
		issuer = "https://api.shoptourr.com",
		accessTokenTtl = Duration.ofMinutes(15),
		refreshTokenTtl = Duration.ofDays(30),
	)
	private val config = JwtConfig(props)
	private val secretKey = config.jwtSecretKey()
	private val encoder: JwtEncoder = config.jwtEncoder(secretKey)
	private val decoder: JwtDecoder = config.jwtDecoder(secretKey)

	@Test
	fun `encoder produces token decoder accepts`() {
		val now = Instant.now()
		val claims = JwtClaimsSet.builder()
			.issuer(props.issuer)
			.issuedAt(now)
			.expiresAt(now.plus(props.accessTokenTtl))
			.subject("user-1")
			.claim("email", "a@b.c")
			.build()
		val headers = JwsHeader.with(MacAlgorithm.HS256).build()
		val token = encoder.encode(JwtEncoderParameters.from(headers, claims)).tokenValue
		assertTrue(token.isNotBlank())

		val decoded = decoder.decode(token)
		assertEquals("user-1", decoded.subject)
		assertEquals(props.issuer, decoded.issuer.toString())
		assertEquals("a@b.c", decoded.getClaimAsString("email"))
	}
}
