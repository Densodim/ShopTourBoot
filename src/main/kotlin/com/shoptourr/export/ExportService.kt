package com.shoptourr.export

import com.shoptourr.ResourceNotFoundException
import com.shoptourr.config.ExportProperties
import com.shoptourr.export.dto.CreateExportRequest
import com.shoptourr.export.dto.ExportFormat
import com.shoptourr.export.dto.ExportJobDto
import com.shoptourr.export.dto.ExportJobStatus
import com.shoptourr.trip.TripService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Instant
import java.util.UUID

@Service
class ExportService(
	private val jobs: ExportJobRepository,
	private val tripService: TripService,
	private val exportProperties: ExportProperties,
	private val clock: Clock,
) {

	@Transactional
	fun create(ownerId: UUID, tripId: UUID, request: CreateExportRequest): ExportJobDto {
		tripService.requireOwned(ownerId, tripId)
		val now = Instant.now(clock)
		val job = ExportJob(
			tripId = tripId,
			ownerId = ownerId,
			format = request.format.name,
			status = ExportJobStatus.READY.name,
			includeTaxFree = request.includeTaxFree,
			includeDiary = request.includeDiary,
			createdAt = now,
			finishedAt = now,
			expiresAt = now.plus(exportProperties.downloadTtl),
		)
		job.downloadUrl = "${exportProperties.publicBaseUrl.trimEnd('/')}/dev-exports/${job.id}"
		return toDto(jobs.save(job), now)
	}

	@Transactional(readOnly = true)
	fun get(ownerId: UUID, exportId: UUID): ExportJobDto {
		val job = jobs.findById(exportId).orElse(null)
		if (job == null || job.ownerId != ownerId) {
			throw ResourceNotFoundException("Export not found.")
		}
		tripService.requireOwned(ownerId, job.tripId)
		return toDto(job, Instant.now(clock))
	}

	private fun toDto(job: ExportJob, now: Instant): ExportJobDto {
		val expired = job.expiresAt?.let { !it.isAfter(now) } == true
		val status = if (expired) ExportJobStatus.EXPIRED else ExportJobStatus.valueOf(job.status)
		return ExportJobDto(
			id = job.id,
			tripId = job.tripId,
			format = ExportFormat.valueOf(job.format),
			status = status,
			downloadUrl = if (status == ExportJobStatus.READY) job.downloadUrl else null,
			expiresAt = job.expiresAt,
			errorCode = job.errorCode,
			createdAt = job.createdAt,
			finishedAt = job.finishedAt,
		)
	}
}
