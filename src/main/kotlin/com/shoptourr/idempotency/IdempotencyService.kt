package com.shoptourr.idempotency

import com.shoptourr.DomainValidationException
import com.shoptourr.IdempotencyConflictException
import com.shoptourr.config.IdempotencyProperties
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.MessageDigest
import java.time.Clock
import java.time.Instant
import java.util.UUID

data class IdempotentReplay(
	val status: Int,
	val body: String,
)

@Service
class IdempotencyService(
	private val records: IdempotencyRecordRepository,
	private val properties: IdempotencyProperties,
	private val clock: Clock,
) {

	private val log = LoggerFactory.getLogger(javaClass)

	@Transactional
	fun replayOrNull(userId: UUID, routeKey: String, key: String, requestHash: String): IdempotentReplay? {
		val normalized = normalizeKey(key)
		val now = Instant.now(clock)
		val existing = records.findByUserIdAndRouteKeyAndIdempotencyKey(userId, routeKey, normalized) ?: return null
		if (!existing.expiresAt.isAfter(now)) {
			records.delete(existing)
			return null
		}
		if (existing.requestHash != requestHash) {
			throw IdempotencyConflictException()
		}
		return IdempotentReplay(existing.responseStatus, existing.responseBody)
	}

	@Transactional
	fun remember(
		userId: UUID,
		routeKey: String,
		key: String,
		requestHash: String,
		status: Int,
		body: String,
	) {
		if (status >= 500) {
			return
		}
		val now = Instant.now(clock)
		try {
			records.save(
				IdempotencyRecord(
					userId = userId,
					routeKey = routeKey,
					idempotencyKey = normalizeKey(key),
					requestHash = requestHash,
					responseStatus = status,
					responseBody = body,
					createdAt = now,
					expiresAt = now.plus(properties.ttl),
				),
			)
		} catch (ex: DataIntegrityViolationException) {
			log.warn("Concurrent idempotency insert for route={}", routeKey, ex)
		}
	}

	companion object {
		const val HEADER = "Idempotency-Key"
		const val MAX_KEY_LENGTH = 64

		fun hashBody(body: ByteArray): String {
			val digest = MessageDigest.getInstance("SHA-256").digest(body)
			return digest.joinToString("") { "%02x".format(it) }
		}

		fun normalizeKey(key: String): String {
			val trimmed = key.trim()
			if (trimmed.isEmpty() || trimmed.length > MAX_KEY_LENGTH) {
				throw DomainValidationException("Idempotency-Key must be 1..$MAX_KEY_LENGTH characters.")
			}
			return trimmed
		}
	}
}
