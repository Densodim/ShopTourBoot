package com.shoptourr.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "voyage.media")
data class MediaProperties(
	val publicBaseUrl: String = "http://localhost:8080",
	val uploadTtl: Duration = Duration.ofMinutes(15),
)
