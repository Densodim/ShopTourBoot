package com.shoptourr.identity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "password_reset_token")
class PasswordResetToken(
	@Id
	val id: UUID = UUID.randomUUID(),

	@Column(name = "user_id", nullable = false)
	val userId: UUID,

	@Column(name = "token_hash", nullable = false, length = 64)
	val tokenHash: String,

	@Column(name = "expires_at", nullable = false)
	val expiresAt: Instant,

	@Column(name = "used_at")
	var usedAt: Instant? = null,

	@Column(name = "created_at", nullable = false)
	val createdAt: Instant,
) {
	fun isUsable(now: Instant): Boolean = usedAt == null && expiresAt.isAfter(now)
}
