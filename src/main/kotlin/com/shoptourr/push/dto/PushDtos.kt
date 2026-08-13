package com.shoptourr.push.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

enum class PushPlatform {
	ANDROID, IOS
}

data class RegisterDeviceRequest(
	@field:NotBlank
	@field:Size(max = 512)
	val token: String,

	@field:NotNull
	val platform: PushPlatform,

	@field:Size(max = 64)
	val appVersion: String? = null,

	@field:Size(max = 120)
	val deviceName: String? = null,
)

data class DeviceDto(
	val id: UUID,
	val tokenFingerprint: String,
	val platform: PushPlatform,
	val appVersion: String?,
	val deviceName: String?,
	val createdAt: Instant,
	val lastSeenAt: Instant?,
)
