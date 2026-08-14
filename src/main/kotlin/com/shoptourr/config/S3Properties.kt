package com.shoptourr.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.nio.file.Path

@ConfigurationProperties(prefix = "voyage.s3")
data class S3Properties(
	val enabled: Boolean = false,
	val endpoint: String = "",
	val region: String = "eu-central-1",
	val bucket: String = "voyage-media",
	val accessKey: String = "",
	val secretKey: String = "",
	val pathStyle: Boolean = true,
	val localDir: String = "",
) {
	fun localRoot(): Path {
		val raw = localDir.trim()
		return if (raw.isEmpty()) {
			Path.of(System.getProperty("java.io.tmpdir"), "voyage-media")
		} else {
			Path.of(raw)
		}
	}
}
