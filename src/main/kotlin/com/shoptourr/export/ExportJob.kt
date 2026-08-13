package com.shoptourr.export

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "export_job")
class ExportJob(
	@Id
	val id: UUID = UUID.randomUUID(),

	@Column(name = "trip_id", nullable = false)
	val tripId: UUID,

	@Column(name = "owner_id", nullable = false)
	val ownerId: UUID,

	@Column(nullable = false, length = 8)
	val format: String,

	@Column(nullable = false, length = 16)
	var status: String,

	@Column(name = "include_tax_free", nullable = false)
	val includeTaxFree: Boolean,

	@Column(name = "include_diary", nullable = false)
	val includeDiary: Boolean,

	@Column(name = "download_url", length = 512)
	var downloadUrl: String? = null,

	@Column(name = "error_code", length = 64)
	var errorCode: String? = null,

	@Column(name = "created_at", nullable = false)
	val createdAt: Instant,

	@Column(name = "finished_at")
	var finishedAt: Instant? = null,

	@Column(name = "expires_at")
	var expiresAt: Instant? = null,
)
