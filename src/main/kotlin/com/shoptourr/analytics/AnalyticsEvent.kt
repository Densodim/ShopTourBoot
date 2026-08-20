package com.shoptourr.analytics

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "analytics_event")
class AnalyticsEvent(
	@Id
	val id: UUID = UUID.randomUUID(),

	@Column(name = "user_id", nullable = false)
	val userId: UUID,

	@Column(name = "client_event_id", nullable = false, length = 64)
	val clientEventId: String,

	@Column(nullable = false, length = 120)
	val name: String,

	@Column(nullable = false, columnDefinition = "TEXT")
	val properties: String,

	@Column(name = "occurred_at", nullable = false)
	val occurredAt: Instant,

	@Column(name = "received_at", nullable = false)
	val receivedAt: Instant,
)
