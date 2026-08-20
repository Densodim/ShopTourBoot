package com.shoptourr.identity

import com.shoptourr.AuthenticationFailedException
import com.shoptourr.config.OAuthProperties
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtException
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

class OidcSocialTokenVerifier(
	private val googleDecoder: JwtDecoder,
	private val appleDecoder: JwtDecoder,
	private val properties: OAuthProperties,
) : SocialTokenVerifier {

	override fun verify(provider: SocialProvider, idToken: String, nonce: String?): SocialIdentity {
		val jwt = decode(provider, idToken)
		validateIssuer(provider, jwt)
		validateAudience(provider, jwt)
		validateNonce(provider, jwt, nonce)
		val subject = jwt.subject?.trim().orEmpty()
		if (subject.isEmpty()) {
			throw AuthenticationFailedException(INVALID_SOCIAL)
		}
		return SocialIdentity(
			provider = provider,
			subject = subject,
			email = jwt.getClaimAsString("email")?.trim()?.lowercase()?.takeIf { it.isNotEmpty() },
			emailVerified = emailVerified(jwt),
			displayName = jwt.getClaimAsString("name")?.trim()?.takeIf { it.isNotEmpty() },
		)
	}

	private fun decode(provider: SocialProvider, idToken: String): Jwt {
		val decoder = when (provider) {
			SocialProvider.GOOGLE -> googleDecoder
			SocialProvider.APPLE -> appleDecoder
		}
		return try {
			decoder.decode(idToken)
		} catch (_: JwtException) {
			throw AuthenticationFailedException(INVALID_SOCIAL)
		} catch (_: IllegalArgumentException) {
			throw AuthenticationFailedException(INVALID_SOCIAL)
		}
	}

	private fun validateIssuer(provider: SocialProvider, jwt: Jwt) {
		val issuer = jwt.getClaimAsString("iss")?.trim().orEmpty()
		val ok = when (provider) {
			SocialProvider.GOOGLE -> issuer in OAuthProperties.GOOGLE_ISSUERS
			SocialProvider.APPLE -> issuer == OAuthProperties.APPLE_ISSUER
		}
		if (!ok) {
			throw AuthenticationFailedException(INVALID_SOCIAL)
		}
	}

	private fun validateAudience(provider: SocialProvider, jwt: Jwt) {
		val allowed = when (provider) {
			SocialProvider.GOOGLE -> properties.googleAudiences()
			SocialProvider.APPLE -> properties.appleAudienceList()
		}
		if (allowed.isEmpty()) {
			throw AuthenticationFailedException(NOT_CONFIGURED)
		}
		val tokenAudiences = buildSet {
			jwt.audience.orEmpty().forEach { add(it) }
			jwt.getClaimAsString("azp")?.trim()?.takeIf { it.isNotEmpty() }?.let { add(it) }
		}
		if (tokenAudiences.none { it in allowed }) {
			throw AuthenticationFailedException(INVALID_SOCIAL)
		}
	}

	private fun validateNonce(provider: SocialProvider, jwt: Jwt, nonce: String?) {
		if (nonce.isNullOrBlank()) {
			return
		}
		val claimed = jwt.getClaimAsString("nonce")?.trim().orEmpty()
		val matches = when (provider) {
			SocialProvider.GOOGLE -> claimed == nonce
			SocialProvider.APPLE -> claimed == nonce || claimed.equals(sha256Hex(nonce), ignoreCase = true)
		}
		if (!matches) {
			throw AuthenticationFailedException(INVALID_SOCIAL)
		}
	}

	private fun emailVerified(jwt: Jwt): Boolean {
		val asBoolean = jwt.getClaimAsBoolean("email_verified")
		if (asBoolean != null) {
			return asBoolean
		}
		return jwt.getClaimAsString("email_verified")?.equals("true", ignoreCase = true) == true
	}

	companion object {
		const val INVALID_SOCIAL = "Social login failed."
		const val NOT_CONFIGURED = "Social login is not configured."

		fun sha256Hex(value: String): String {
			val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(StandardCharsets.UTF_8))
			return digest.joinToString("") { byte -> "%02x".format(byte) }
		}
	}
}
