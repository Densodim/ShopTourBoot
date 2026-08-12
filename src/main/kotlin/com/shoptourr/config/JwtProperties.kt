package com.shoptourr.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration
import java.time.Duration

@Configuration
@EnableConfigurationProperties(JwtProperties::class)
class AppConfig

@ConfigurationProperties(prefix = "voyage.jwt")
data class JwtProperties(
	val secret: String,
	val issuer: String = "https://api.shoptourr.com",
	val accessTokenTtl: Duration = Duration.ofMinutes(15),
	val refreshTokenTtl: Duration = Duration.ofDays(30),
)
