package com.shoptourr.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "voyage.ocr")
data class OcrProperties(
	val enabled: Boolean = true,
	val baseUrl: String = "https://api.ocr.space",
	val apiKey: String = "",
	val language: String = "eng",
	val cacheTtl: Duration = Duration.ofDays(7),
	val connectTimeout: Duration = Duration.ofSeconds(2),
	val readTimeout: Duration = Duration.ofSeconds(15),
	val maxBytes: Long = 1_000_000,
)
