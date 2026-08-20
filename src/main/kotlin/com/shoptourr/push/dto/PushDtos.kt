package com.shoptourr.push.dto

import com.shoptourr.shared.validation.FieldPatterns
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

enum class PushPlatform {
	ANDROID, IOS
}

data class RegisterDeviceRequest(
	@field:NotBlank
	@field:Size(max = 512)
	@field:Pattern(regexp = FieldPatterns.PUSH_TOKEN)
	val token: String,

	@field:NotNull
	val platform: PushPlatform,

	@field:Size(max = 64)
	@field:Pattern(regexp = FieldPatterns.APP_VERSION)
	val appVersion: String? = null,

	@field:Size(max = 120)
	@field:Pattern(regexp = FieldPatterns.DEVICE_NAME)
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
