package com.shoptourr.identity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "refresh_token")
class RefreshToken(
	@Id
	val id: UUID = UUID.randomUUID(),

	@Column(name = "user_id", nullable = false)
	val userId: UUID,

	@Column(name = "token_hash", nullable = false, length = 64)
	val tokenHash: String,

	@Column(name = "device_name", length = 120)
	val deviceName: String? = null,

	@Column(name = "expires_at", nullable = false)
	val expiresAt: Instant,

	@Column(name = "revoked_at")
	var revokedAt: Instant? = null,

	@Column(name = "created_at", nullable = false)
	val createdAt: Instant,
) {
	fun isActive(now: Instant): Boolean = revokedAt == null && expiresAt.isAfter(now)
}
