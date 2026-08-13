package com.shoptourr.export.dto

import jakarta.validation.constraints.NotNull
import java.time.Instant
import java.util.UUID

enum class ExportFormat {
	PDF, CSV
}

enum class ExportJobStatus {
	QUEUED, RUNNING, READY, FAILED, EXPIRED
}

data class CreateExportRequest(
	@field:NotNull
	val format: ExportFormat,
	val includeTaxFree: Boolean = false,
	val includeDiary: Boolean = false,
)

data class ExportJobDto(
	val id: UUID,
	val tripId: UUID,
	val format: ExportFormat,
	val status: ExportJobStatus,
	val downloadUrl: String?,
	val expiresAt: Instant?,
	val errorCode: String?,
	val createdAt: Instant,
	val finishedAt: Instant?,
)
