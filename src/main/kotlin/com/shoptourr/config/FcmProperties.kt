package com.shoptourr.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "voyage.fcm")
data class FcmProperties(
	val enabled: Boolean = true,
	val credentials: String = "",
	val tokenUrl: String = "https://oauth2.googleapis.com/token",
	val sendUrl: String = "https://fcm.googleapis.com/v1/projects/{projectId}/messages:send",
	val connectTimeout: Duration = Duration.ofSeconds(2),
	val readTimeout: Duration = Duration.ofSeconds(10),
)
