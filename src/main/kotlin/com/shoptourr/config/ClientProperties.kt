package com.shoptourr.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "voyage.client")
data class ClientProperties(
	val minAndroidBuild: Int = 1,
	val minIosBuild: Int = 1,
	val softMinAndroidBuild: Int? = null,
	val softMinIosBuild: Int? = null,
	val flags: Flags = Flags(),
	val storeUrlAndroid: String? = null,
	val storeUrlIos: String? = null,
) {
	data class Flags(
		val exportPdf: Boolean = true,
		val ocrAssist: Boolean = true,
		val nativeMaps: Boolean = false,
	)
}
