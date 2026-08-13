package com.shoptourr.media

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "media_asset")
class MediaAsset(
	@Id
	val id: UUID = UUID.randomUUID(),

	@Column(name = "user_id", nullable = false)
	val userId: UUID,

	@Column(nullable = false, length = 16)
	val purpose: String,

	@Column(nullable = false, length = 16)
	var status: String,

	@Column(name = "content_type", nullable = false, length = 128)
	val contentType: String,

	@Column(name = "byte_size", nullable = false)
	val byteSize: Long,

	@Column(name = "sha256_hex", length = 64)
	val sha256Hex: String? = null,

	@Column(name = "created_at", nullable = false)
	val createdAt: Instant,

	@Column(name = "updated_at", nullable = false)
	var updatedAt: Instant,

	@Column(name = "deleted_at")
	var deletedAt: Instant? = null,
)
