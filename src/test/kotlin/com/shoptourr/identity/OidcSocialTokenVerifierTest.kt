package com.shoptourr.identity

import com.shoptourr.AuthenticationFailedException
import com.shoptourr.config.OAuthProperties
import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator
import com.nimbusds.jose.jwk.source.ImmutableJWKSet
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm
import org.springframework.security.oauth2.jwt.JwsHeader
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.security.oauth2.jwt.JwtEncoderParameters
import org.springframework.security.oauth2.jwt.JwtValidators
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder
import java.time.Instant

class OidcSocialTokenVerifierTest {

	private val rsa = RSAKeyGenerator(2048).keyID("kid-1").generate()
	private val encoder = NimbusJwtEncoder(ImmutableJWKSet(JWKSet(rsa)))
	private val decoder = NimbusJwtDecoder.withPublicKey(rsa.toRSAPublicKey()).build().apply {
		setJwtValidator(JwtValidators.createDefault())
	}
	private val properties = OAuthProperties(
		googleClientIds = "web-client.apps.googleusercontent.com",
		appleAudiences = "com.shoptourr",
	)
	private val verifier = OidcSocialTokenVerifier(decoder, decoder, properties)

	@Test
	fun `google token with matching audience and nonce is accepted`() {
		val token = sign(
			issuer = "https://accounts.google.com",
			audience = "web-client.apps.googleusercontent.com",
			subject = "google-sub-1",
			email = "Ada@Example.com",
			emailVerified = true,
			name = "Ada",
			nonce = "n-1",
		)

		val identity = verifier.verify(SocialProvider.GOOGLE, token, "n-1")

		assertEquals("google-sub-1", identity.subject)
		assertEquals("ada@example.com", identity.email)
		assertTrue(identity.emailVerified)
		assertEquals("Ada", identity.displayName)
	}

	@Test
	fun `apple nonce may be the sha256 of the raw value`() {
		val raw = "apple-nonce"
		val token = sign(
			issuer = "https://appleid.apple.com",
			audience = "com.shoptourr",
			subject = "apple-sub-1",
			email = "ada@privaterelay.appleid.com",
			emailVerified = true,
			nonce = OidcSocialTokenVerifier.sha256Hex(raw),
		)

		val identity = verifier.verify(SocialProvider.APPLE, token, raw)

		assertEquals("apple-sub-1", identity.subject)
	}

	@Test
	fun `wrong audience is rejected`() {
		val token = sign(
			issuer = "https://accounts.google.com",
			audience = "other-client",
			subject = "google-sub-1",
			email = "ada@example.com",
			emailVerified = true,
		)

		assertThrows<AuthenticationFailedException> {
			verifier.verify(SocialProvider.GOOGLE, token, null)
		}
	}

	@Test
	fun `garbage token is rejected without leaking jwt details`() {
		val ex = assertThrows<AuthenticationFailedException> {
			verifier.verify(SocialProvider.GOOGLE, "not-a-jwt", null)
		}
		assertEquals(OidcSocialTokenVerifier.INVALID_SOCIAL, ex.message)
	}

	private fun sign(
		issuer: String,
		audience: String,
		subject: String,
		email: String,
		emailVerified: Boolean,
		name: String? = null,
		nonce: String? = null,
	): String {
		val now = Instant.parse("2026-08-20T12:00:00Z")
		val claims = JwtClaimsSet.builder()
			.issuer(issuer)
			.audience(listOf(audience))
			.subject(subject)
			.issuedAt(now)
			.expiresAt(now.plusSeconds(3600))
			.claim("email", email)
			.claim("email_verified", emailVerified)
			.apply {
				if (name != null) claim("name", name)
				if (nonce != null) claim("nonce", nonce)
			}
			.build()
		val headers = JwsHeader.with(SignatureAlgorithm.RS256).keyId("kid-1").build()
		return encoder.encode(JwtEncoderParameters.from(headers, claims)).tokenValue
	}
}
