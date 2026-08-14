package com.shoptourr.export

import com.shoptourr.ResourceNotFoundException
import com.shoptourr.config.ExportProperties
import com.shoptourr.diary.DiaryEntryRepository
import com.shoptourr.export.dto.CreateExportRequest
import com.shoptourr.export.dto.ExportFormat
import com.shoptourr.export.dto.ExportJobDto
import com.shoptourr.export.dto.ExportJobStatus
import com.shoptourr.insights.TaxFreeCatalog
import com.shoptourr.purchase.PurchaseRepository
import com.shoptourr.trip.TripService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.servlet.support.ServletUriComponentsBuilder
import java.math.RoundingMode
import java.time.Clock
import java.time.Instant
import java.util.UUID

@Service
class ExportService(
	private val jobs: ExportJobRepository,
	private val tripService: TripService,
	private val purchases: PurchaseRepository,
	private val diaryEntries: DiaryEntryRepository,
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
		job.downloadUrl = "${publicBaseUrl()}/dev-exports/${job.id}"
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

	@Transactional(readOnly = true)
	fun loadFile(exportId: UUID): StoredExport {
		val now = Instant.now(clock)
		val job = jobs.findById(exportId).orElse(null)
			?: throw ResourceNotFoundException("Export not found.")
		if (job.expiresAt?.let { !it.isAfter(now) } == true) {
			throw ResourceNotFoundException("Export not found.")
		}
		val trip = tripService.requireOwned(job.ownerId, job.tripId)
		val taxFreeRules = if (job.includeTaxFree) TaxFreeCatalog.rules(trip.countryCode) else null
		val purchaseRows = purchases
			.findAllByTripIdAndDeletedAtIsNullOrderByPurchaseDateDescPurchaseTimeDesc(job.tripId)
			.map { purchase ->
				val amount = purchase.grossAmount.setScale(2, RoundingMode.HALF_UP)
				val estimatedRefund = if (taxFreeRules != null && purchase.taxRefundEligible) {
					amount.multiply(taxFreeRules.rate).setScale(2, RoundingMode.HALF_UP)
				} else {
					null
				}
				val meetsMinimum = if (taxFreeRules != null) {
					purchase.taxRefundEligible && amount >= taxFreeRules.minimum
				} else {
					null
				}
				ExportCsv.PurchaseRow(
					id = purchase.id,
					name = purchase.name,
					category = purchase.category,
					date = purchase.purchaseDate,
					time = purchase.purchaseTime,
					place = purchase.place,
					gross = purchase.grossAmount,
					net = purchase.netAmount,
					vat = purchase.vatAmount,
					vatRate = purchase.vatRatePercent,
					currency = purchase.currency,
					taxRefundEligible = purchase.taxRefundEligible,
					estimatedRefund = estimatedRefund,
					meetsMinimum = meetsMinimum,
				)
			}
		val diaryRows = if (job.includeDiary) {
			diaryEntries.findAllByTripIdAndDeletedAtIsNullOrderByEntryDateDescCreatedAtDesc(job.tripId)
				.map { entry -> ExportCsv.DiaryRow(entry.entryDate, entry.mood, entry.text) }
		} else {
			null
		}
		val taxFree = taxFreeRules?.let { rules ->
			ExportCsv.TaxFreeBlock(
				currency = trip.budgetCurrency,
				minimum = rules.minimum,
				refundRate = rules.rate,
				region = trip.country,
			)
		}
		val csv = ExportCsv.render(purchaseRows, diaryRows, taxFree)
		val title = "${trip.city}, ${trip.country}"
		return when (ExportFormat.valueOf(job.format)) {
			ExportFormat.CSV -> StoredExport(
				filename = "export-${job.id}.csv",
				contentType = "text/csv; charset=UTF-8",
				bytes = csv.toByteArray(Charsets.UTF_8),
			)
			ExportFormat.PDF -> StoredExport(
				filename = "export-${job.id}.pdf",
				contentType = "application/pdf",
				bytes = ExportPdf.render(title, csv),
			)
		}
	}

	private fun publicBaseUrl(): String {
		return try {
			val fromRequest = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString()
			fromRequest.ifBlank { exportProperties.publicBaseUrl.trimEnd('/') }
		} catch (_: IllegalStateException) {
			exportProperties.publicBaseUrl.trimEnd('/')
		}
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

data class StoredExport(
	val filename: String,
	val contentType: String,
	val bytes: ByteArray,
)
