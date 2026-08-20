package com.shoptourr.identity

fun interface SocialTokenVerifier {
	fun verify(provider: SocialProvider, idToken: String, nonce: String?): SocialIdentity
}

data class SocialIdentity(
	val provider: SocialProvider,
	val subject: String,
	val email: String?,
	val emailVerified: Boolean,
	val displayName: String?,
)
