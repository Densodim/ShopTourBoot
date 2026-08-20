package com.shoptourr.config

import com.shoptourr.identity.OidcSocialTokenVerifier
import com.shoptourr.identity.SocialTokenVerifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.jwt.JwtValidators
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder

@Configuration
class OAuthConfig(
	private val properties: OAuthProperties,
) {
	@Bean
	fun socialTokenVerifier(): SocialTokenVerifier {
		val google = NimbusJwtDecoder.withJwkSetUri(properties.googleJwkSetUri).build().apply {
			setJwtValidator(JwtValidators.createDefault())
		}
		val apple = NimbusJwtDecoder.withJwkSetUri(properties.appleJwkSetUri).build().apply {
			setJwtValidator(JwtValidators.createDefault())
		}
		return OidcSocialTokenVerifier(google, apple, properties)
	}
}
