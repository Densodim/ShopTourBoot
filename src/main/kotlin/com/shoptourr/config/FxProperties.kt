package com.shoptourr.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "voyage.fx")
data class FxProperties(
	val enabled: Boolean = true,
	val baseUrl: String = "https://open.er-api.com/v6/latest",
	val cacheTtl: Duration = Duration.ofHours(1),
	val connectTimeout: Duration = Duration.ofSeconds(2),
	val readTimeout: Duration = Duration.ofSeconds(3),
)
