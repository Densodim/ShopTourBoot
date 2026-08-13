package com.shoptourr.push

import com.shoptourr.ResourceNotFoundException
import com.shoptourr.identity.TokenService
import com.shoptourr.push.dto.DeviceDto
import com.shoptourr.push.dto.PushPlatform
import com.shoptourr.push.dto.RegisterDeviceRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Instant
import java.util.UUID

@Service
class PushService(
	private val devices: PushDeviceRepository,
	private val tokenService: TokenService,
	private val clock: Clock,
) {

	@Transactional
	fun register(userId: UUID, request: RegisterDeviceRequest): DeviceDto {
		val now = Instant.now(clock)
		val tokenHash = tokenService.hash(request.token)
		val existing = devices.findByUserIdAndTokenHashAndDeletedAtIsNull(userId, tokenHash)
		val device = if (existing != null) {
			existing.platform = request.platform.name
			existing.appVersion = request.appVersion
			existing.deviceName = request.deviceName?.trim()?.takeIf { it.isNotBlank() }
			existing.lastSeenAt = now
			existing
		} else {
			devices.save(
				PushDevice(
					userId = userId,
					tokenHash = tokenHash,
					platform = request.platform.name,
					appVersion = request.appVersion,
					deviceName = request.deviceName?.trim()?.takeIf { it.isNotBlank() },
					createdAt = now,
					lastSeenAt = now,
				),
			)
		}
		return toDto(device)
	}

	@Transactional
	fun unregister(userId: UUID, deviceId: UUID) {
		val device = devices.findByIdAndDeletedAtIsNull(deviceId)
		if (device == null || device.userId != userId) {
			throw ResourceNotFoundException("Device not found.")
		}
		device.deletedAt = Instant.now(clock)
	}

	private fun toDto(device: PushDevice): DeviceDto =
		DeviceDto(
			id = device.id,
			tokenFingerprint = device.tokenHash.take(8),
			platform = PushPlatform.valueOf(device.platform),
			appVersion = device.appVersion,
			deviceName = device.deviceName,
			createdAt = device.createdAt,
			lastSeenAt = device.lastSeenAt,
		)
}
