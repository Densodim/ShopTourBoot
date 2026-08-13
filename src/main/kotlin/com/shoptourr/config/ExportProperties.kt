package com.shoptourr.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "voyage.export")
data class ExportProperties(
	val publicBaseUrl: String = "http://localhost:8080",
	val downloadTtl: Duration = Duration.ofHours(24),
)
