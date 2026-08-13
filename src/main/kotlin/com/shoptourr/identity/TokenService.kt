package com.shoptourr.identity

import com.shoptourr.config.JwtProperties
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.JwtEncoderParameters
import org.springframework.security.oauth2.jwt.JwsHeader
import org.springframework.stereotype.Service
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Clock
import java.time.Instant
import java.util.Base64
import java.util.UUID

@Service
class TokenService(
	private val jwtEncoder: JwtEncoder,
	private val jwtProperties: JwtProperties,
	private val clock: Clock,
) {

	private val random = SecureRandom()

	fun issueAccessToken(user: AppUser, sessionId: UUID): String {
		val now = Instant.now(clock)
		val claims = JwtClaimsSet.builder()
			.issuer(jwtProperties.issuer)
			.issuedAt(now)
			.expiresAt(now.plus(jwtProperties.accessTokenTtl))
			.subject(user.id.toString())
			.claim("email", user.email)
			.claim(SESSION_CLAIM, sessionId.toString())
			.build()
		val headers = JwsHeader.with(MacAlgorithm.HS256).build()
		return jwtEncoder.encode(JwtEncoderParameters.from(headers, claims)).tokenValue
	}

	fun newRefreshTokenValue(): String {
		val bytes = ByteArray(32)
		random.nextBytes(bytes)
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
	}

	fun hash(token: String): String {
		val digest = MessageDigest.getInstance("SHA-256").digest(token.toByteArray(Charsets.UTF_8))
		return digest.joinToString("") { "%02x".format(it) }
	}

	fun accessExpiresInSeconds(): Long = jwtProperties.accessTokenTtl.toSeconds()

	fun refreshExpiresAt(now: Instant): Instant = now.plus(jwtProperties.refreshTokenTtl)

	fun refreshExpiresInSeconds(): Long = jwtProperties.refreshTokenTtl.toSeconds()

	companion object {
		const val SESSION_CLAIM = "sid"
	}
}
