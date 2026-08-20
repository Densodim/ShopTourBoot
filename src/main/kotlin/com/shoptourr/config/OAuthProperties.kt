package com.shoptourr.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "voyage.oauth")
data class OAuthProperties(
	val googleClientIds: String = "",
	val appleAudiences: String = "",
	val googleJwkSetUri: String = "https://www.googleapis.com/oauth2/v3/certs",
	val appleJwkSetUri: String = "https://appleid.apple.com/auth/keys",
) {
	fun googleAudiences(): List<String> = csv(googleClientIds)

	fun appleAudienceList(): List<String> = csv(appleAudiences)

	private fun csv(raw: String): List<String> =
		raw.split(',').map { it.trim() }.filter { it.isNotEmpty() }

	companion object {
		val GOOGLE_ISSUERS = setOf("https://accounts.google.com", "accounts.google.com")
		const val APPLE_ISSUER = "https://appleid.apple.com"
	}
}
