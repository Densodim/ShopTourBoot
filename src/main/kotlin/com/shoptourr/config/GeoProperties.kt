package com.shoptourr.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "voyage.geo")
data class GeoProperties(
	val enabled: Boolean = true,
	val baseUrl: String = "https://nominatim.openstreetmap.org",
	val userAgent: String = "ShopTourr/1.0 (https://github.com/Densodim/ShopTourBoot)",
	val cacheTtl: Duration = Duration.ofDays(7),
	val connectTimeout: Duration = Duration.ofSeconds(2),
	val readTimeout: Duration = Duration.ofSeconds(3),
)
