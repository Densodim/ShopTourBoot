package com.shoptourr.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "voyage.idempotency")
data class IdempotencyProperties(
	val ttl: Duration = Duration.ofHours(24),
)
