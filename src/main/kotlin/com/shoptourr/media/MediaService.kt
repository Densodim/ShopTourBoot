package com.shoptourr.media

import com.shoptourr.DomainValidationException
import com.shoptourr.MediaNotReadyException
import com.shoptourr.ResourceNotFoundException
import com.shoptourr.config.MediaProperties
import com.shoptourr.media.dto.ConfirmMediaUploadRequest
import com.shoptourr.media.dto.CreateMediaUploadIntentRequest
import com.shoptourr.media.dto.MediaAssetDto
import com.shoptourr.media.dto.MediaPurpose
import com.shoptourr.media.dto.MediaStatus
import com.shoptourr.media.dto.MediaUploadIntentResponse
import com.shoptourr.media.dto.ReceiptOcrResultDto
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.servlet.support.ServletUriComponentsBuilder
import java.time.Clock
import java.time.Instant
import java.util.UUID

@Service
class MediaService(
	private val assets: MediaAssetRepository,
	private val mediaProperties: MediaProperties,
	private val clock: Clock,
) {

	@Transactional
	fun createIntent(userId: UUID, request: CreateMediaUploadIntentRequest): MediaUploadIntentResponse {
		if (request.byteSize > MAX_BYTES) {
			throw DomainValidationException("File exceeds 12MB limit.")
		}
		val now = Instant.now(clock)
		val asset = assets.save(
			MediaAsset(
				userId = userId,
				purpose = request.purpose.name,
				status = MediaStatus.PENDING_UPLOAD.name,
				contentType = request.contentType.trim(),
				byteSize = request.byteSize,
				sha256Hex = request.sha256Hex,
				createdAt = now,
				updatedAt = now,
			),
		)
		return MediaUploadIntentResponse(
			mediaId = asset.id,
			uploadUrl = "${publicBaseUrl()}/dev-uploads/${asset.id}",
			requiredHeaders = mapOf("Content-Type" to asset.contentType),
			uploadExpiresAt = now.plus(mediaProperties.uploadTtl),
			status = MediaStatus.PENDING_UPLOAD,
		)
	}

	@Transactional
	fun storeBytes(mediaId: UUID, body: ByteArray) {
		val asset = assets.findByIdAndDeletedAtIsNull(mediaId)
			?: throw ResourceNotFoundException("Media not found.")
		if (body.isEmpty()) {
			throw DomainValidationException("Empty upload.")
		}
		if (body.size > MAX_BYTES) {
			throw DomainValidationException("File exceeds 12MB limit.")
		}
		asset.content = body
		asset.byteSize = body.size.toLong()
		asset.status = MediaStatus.UPLOADED.name
		asset.updatedAt = Instant.now(clock)
	}

	@Transactional(readOnly = true)
	fun loadBytes(mediaId: UUID): StoredMedia {
		val asset = assets.findByIdAndDeletedAtIsNull(mediaId)
			?: throw ResourceNotFoundException("Media not found.")
		val bytes = asset.content ?: throw MediaNotReadyException()
		return StoredMedia(asset.contentType, bytes)
	}

	@Transactional
	fun confirm(userId: UUID, mediaId: UUID, request: ConfirmMediaUploadRequest): MediaAssetDto {
		val asset = requireOwned(userId, mediaId)
		if (request.uploaded) {
			asset.status = MediaStatus.READY.name
			asset.updatedAt = Instant.now(clock)
		}
		return toDto(asset)
	}

	@Transactional(readOnly = true)
	fun get(userId: UUID, mediaId: UUID): MediaAssetDto = toDto(requireOwned(userId, mediaId))

	@Transactional(readOnly = true)
	fun ocr(userId: UUID, mediaId: UUID): ReceiptOcrResultDto {
		val asset = requireOwned(userId, mediaId)
		if (asset.status != MediaStatus.READY.name) {
			throw MediaNotReadyException()
		}
		return if (asset.purpose == MediaPurpose.RECEIPT.name) {
			ReceiptOcrResultDto(
				mediaId = asset.id,
				suggestedName = "Receipt",
				suggestedAmount = "0.00",
				suggestedPlace = null,
				suggestedCategory = "OTHER",
				confidence = 0.1,
			)
		} else {
			ReceiptOcrResultDto(asset.id, null, null, null, null, 0.0)
		}
	}

	private fun requireOwned(userId: UUID, mediaId: UUID): MediaAsset {
		val asset = assets.findByIdAndDeletedAtIsNull(mediaId)
		if (asset == null || asset.userId != userId) {
			throw ResourceNotFoundException("Media not found.")
		}
		return asset
	}

	private fun toDto(asset: MediaAsset): MediaAssetDto {
		val ready = asset.status == MediaStatus.READY.name
		val url = "${publicBaseUrl()}/dev-uploads/${asset.id}"
		return MediaAssetDto(
			id = asset.id,
			purpose = MediaPurpose.valueOf(asset.purpose),
			status = MediaStatus.valueOf(asset.status),
			contentType = asset.contentType,
			byteSize = asset.byteSize,
			downloadUrl = if (ready) url else null,
			thumbnailUrl = if (ready) url else null,
			createdAt = asset.createdAt,
		)
	}

	private fun publicBaseUrl(): String {
		return try {
			val fromRequest = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString()
			fromRequest.ifBlank { mediaProperties.publicBaseUrl.trimEnd('/') }
		} catch (_: IllegalStateException) {
			mediaProperties.publicBaseUrl.trimEnd('/')
		}
	}

	companion object {
		const val MAX_BYTES = 12L * 1024 * 1024
	}
}

data class StoredMedia(
	val contentType: String,
	val bytes: ByteArray,
)
