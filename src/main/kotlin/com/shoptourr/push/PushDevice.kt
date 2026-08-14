package com.shoptourr.push

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "push_device")
class PushDevice(
	@Id
	val id: UUID = UUID.randomUUID(),

	@Column(name = "user_id", nullable = false)
	val userId: UUID,

	@Column(name = "token_hash", nullable = false, length = 64)
	val tokenHash: String,

	@Column(length = 512)
	var token: String? = null,

	@Column(nullable = false, length = 16)
	var platform: String,

	@Column(name = "app_version", length = 64)
	var appVersion: String? = null,

	@Column(name = "device_name", length = 120)
	var deviceName: String? = null,

	@Column(name = "created_at", nullable = false)
	val createdAt: Instant,

	@Column(name = "last_seen_at", nullable = false)
	var lastSeenAt: Instant,

	@Column(name = "deleted_at")
	var deletedAt: Instant? = null,
)
