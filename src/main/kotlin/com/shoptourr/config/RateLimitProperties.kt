package com.shoptourr.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "voyage.rate-limit")
data class RateLimitProperties(
	val enabled: Boolean = true,
	val window: Duration = Duration.ofMinutes(1),
	val authPerWindow: Int = 20,
	val apiPerWindow: Int = 300,
)
