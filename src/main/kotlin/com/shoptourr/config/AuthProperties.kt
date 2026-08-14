package com.shoptourr.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "voyage.auth")
data class AuthProperties(
	val resetTokenTtl: Duration = Duration.ofMinutes(30),
)
