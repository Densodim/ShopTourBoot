package com.shoptourr.push

import com.shoptourr.ResourceNotFoundException
import com.shoptourr.identity.AppUserRepository
import com.shoptourr.identity.TokenService
import com.shoptourr.push.dto.DeviceDto
import com.shoptourr.push.dto.PushPlatform
import com.shoptourr.push.dto.RegisterDeviceRequest
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.util.UUID

@Service
class PushService(
	private val devices: PushDeviceRepository,
	private val users: AppUserRepository,
	private val tokenService: TokenService,
	private val fcm: LiveFcmClient,
	private val clock: Clock,
) {

	private val log = LoggerFactory.getLogger(javaClass)

	@Transactional
	fun register(userId: UUID, request: RegisterDeviceRequest): DeviceDto {
		val now = Instant.now(clock)
		val tokenHash = tokenService.hash(request.token)
		val existing = devices.findByUserIdAndTokenHashAndDeletedAtIsNull(userId, tokenHash)
		val device = if (existing != null) {
			existing.platform = request.platform.name
			existing.appVersion = request.appVersion
			existing.deviceName = request.deviceName?.trim()?.takeIf { it.isNotBlank() }
			existing.token = request.token
			existing.lastSeenAt = now
			existing
		} else {
			devices.save(
				PushDevice(
					userId = userId,
					tokenHash = tokenHash,
					token = request.token,
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

	@Transactional
	fun notifyBudgetCrossing(
		userId: UUID,
		tripId: UUID,
		spentBefore: BigDecimal,
		spentAfter: BigDecimal,
		budget: BigDecimal,
	) {
		val type = crossing(spentBefore, spentAfter, budget) ?: return
		val user = users.findById(userId).orElse(null) ?: return
		if (!user.pushNotificationsEnabled) {
			return
		}
		val message = messageFor(type, tripId)
		devices.findAllByUserIdAndDeletedAtIsNull(userId).forEach { device ->
			val token = device.token ?: return@forEach
			when (fcm.send(token, message.title, message.body, message.data)) {
				FcmSendResult.UNREGISTERED -> device.deletedAt = Instant.now(clock)
				FcmSendResult.FAILED -> log.warn("FCM delivery failed for device={}", device.id)
				FcmSendResult.SENT, FcmSendResult.SKIPPED -> Unit
			}
		}
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

	companion object {
		private val ALMOST = BigDecimal("0.80")

		fun crossing(before: BigDecimal, after: BigDecimal, budget: BigDecimal): String? {
			if (budget.signum() <= 0) {
				return null
			}
			val almost = budget.multiply(ALMOST)
			return when {
				before <= budget && after > budget -> "BUDGET_EXCEEDED"
				before < almost && after >= almost -> "BUDGET_ALMOST_GONE"
				else -> null
			}
		}

		private fun messageFor(type: String, tripId: UUID): PushNotice {
			val (title, body, titleKey, bodyKey) = when (type) {
				"BUDGET_EXCEEDED" -> NoticeCopy(
					"Budget exceeded",
					"This trip is over budget.",
					"alert.budget_exceeded.title",
					"alert.budget_exceeded.body",
				)
				else -> NoticeCopy(
					"Budget running low",
					"You've used 80% of this trip's budget.",
					"alert.budget_almost_gone.title",
					"alert.budget_almost_gone.body",
				)
			}
			return PushNotice(
				title = title,
				body = body,
				data = mapOf(
					"type" to type,
					"tripId" to tripId.toString(),
					"titleKey" to titleKey,
					"bodyKey" to bodyKey,
				),
			)
		}
	}

	private data class NoticeCopy(
		val title: String,
		val body: String,
		val titleKey: String,
		val bodyKey: String,
	)

	private data class PushNotice(
		val title: String,
		val body: String,
		val data: Map<String, String>,
	)
}
