package com.shoptourr.analytics

import com.shoptourr.DomainValidationException
import com.shoptourr.analytics.dto.AnalyticsBatchRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.json.JsonMapper
import java.time.Clock
import java.time.Instant
import java.util.UUID

@Service
class AnalyticsService(
	private val events: AnalyticsEventRepository,
	private val jsonMapper: JsonMapper,
	private val clock: Clock,
) {

	@Transactional
	fun ingest(userId: UUID, request: AnalyticsBatchRequest): Int {
		val receivedAt = Instant.now(clock)
		var stored = 0
		for (item in request.events) {
			val occurredAt = parseTimestamp(item.timestamp)
			if (events.existsByUserIdAndClientEventId(userId, item.id)) {
				continue
			}
			events.save(
				AnalyticsEvent(
					userId = userId,
					clientEventId = item.id,
					name = item.name,
					properties = jsonMapper.writeValueAsString(item.properties),
					occurredAt = occurredAt,
					receivedAt = receivedAt,
				),
			)
			stored += 1
		}
		return stored
	}

	private fun parseTimestamp(raw: String): Instant =
		runCatching { Instant.parse(raw) }.getOrElse {
			throw DomainValidationException("timestamp must be ISO-8601 UTC")
		}
}
