package com.shoptourr.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "voyage.jwt")
data class JwtProperties(
	val secret: String,
	val issuer: String = "https://api.shoptourr.com",
	val accessTokenTtl: Duration = Duration.ofMinutes(15),
	val refreshTokenTtl: Duration = Duration.ofDays(30),
)
