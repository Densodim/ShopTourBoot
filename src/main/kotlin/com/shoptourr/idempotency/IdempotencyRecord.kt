package com.shoptourr.idempotency

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "idempotency_record")
class IdempotencyRecord(
	@Id
	val id: UUID = UUID.randomUUID(),

	@Column(name = "user_id", nullable = false)
	val userId: UUID,

	@Column(name = "route_key", nullable = false, length = 255)
	val routeKey: String,

	@Column(name = "idempotency_key", nullable = false, length = 64)
	val idempotencyKey: String,

	@Column(name = "request_hash", nullable = false, length = 64)
	val requestHash: String,

	@Column(name = "response_status", nullable = false)
	val responseStatus: Int,

	@Column(name = "response_body", nullable = false, columnDefinition = "TEXT")
	val responseBody: String,

	@Column(name = "created_at", nullable = false)
	val createdAt: Instant,

	@Column(name = "expires_at", nullable = false)
	val expiresAt: Instant,
)
