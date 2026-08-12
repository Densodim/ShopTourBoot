package com.shoptourr.config

import com.nimbusds.jose.jwk.source.ImmutableSecret
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

@Configuration
class JwtConfig(
	private val jwtProperties: JwtProperties,
) {
	@Bean
	fun jwtSecretKey(): SecretKey {
		val bytes = jwtProperties.secret.toByteArray(Charsets.UTF_8)
		require(bytes.size >= 32) {
			"voyage.jwt.secret must be at least 32 bytes for HS256"
		}
		return SecretKeySpec(bytes, "HmacSHA256")
	}

	@Bean
	fun jwtEncoder(secretKey: SecretKey): JwtEncoder =
		NimbusJwtEncoder(ImmutableSecret(secretKey))

	@Bean
	fun jwtDecoder(secretKey: SecretKey): JwtDecoder =
		NimbusJwtDecoder.withSecretKey(secretKey)
			.macAlgorithm(MacAlgorithm.HS256)
			.build()
}
