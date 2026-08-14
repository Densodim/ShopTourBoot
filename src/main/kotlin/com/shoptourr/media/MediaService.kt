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
import java.security.MessageDigest
import java.time.Clock
import java.time.Instant
import java.util.UUID

@Service
class MediaService(
	private val assets: MediaAssetRepository,
	private val blobs: MediaBlobStore,
	private val ocr: OcrService,
	private val mediaProperties: MediaProperties,
	private val clock: Clock,
) {

	@Transactional
	fun createIntent(userId: UUID, request: CreateMediaUploadIntentRequest): MediaUploadIntentResponse {
		if (request.byteSize > MAX_BYTES) {
			throw DomainValidationException("File exceeds 12MB limit.")
		}
		val now = Instant.now(clock)
		val expiresAt = now.plus(mediaProperties.uploadTtl)
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
				uploadExpiresAt = expiresAt,
			),
		)
		return MediaUploadIntentResponse(
			mediaId = asset.id,
			uploadUrl = "${publicBaseUrl()}/dev-uploads/${asset.id}",
			requiredHeaders = mapOf("Content-Type" to asset.contentType),
			uploadExpiresAt = expiresAt,
			status = MediaStatus.PENDING_UPLOAD,
		)
	}

	@Transactional
	fun storeBytes(mediaId: UUID, body: ByteArray) {
		val asset = assets.findByIdAndDeletedAtIsNull(mediaId)
			?: throw ResourceNotFoundException("Media not found.")
		if (asset.status == MediaStatus.READY.name) {
			throw DomainValidationException("Media is already confirmed.")
		}
		val now = Instant.now(clock)
		if (asset.uploadExpiresAt?.let { !now.isBefore(it) } == true) {
			throw DomainValidationException("Upload URL has expired.")
		}
		if (body.isEmpty()) {
			throw DomainValidationException("Empty upload.")
		}
		if (body.size > MAX_BYTES) {
			throw DomainValidationException("File exceeds 12MB limit.")
		}
		asset.sha256Hex?.let { expected ->
			if (!expected.equals(sha256Hex(body), ignoreCase = true)) {
				throw DomainValidationException("Upload hash does not match sha256Hex.")
			}
		}
		val key = keyFor(asset)
		blobs.put(key, asset.contentType, body)
		asset.storageKey = key
		asset.content = null
		asset.byteSize = body.size.toLong()
		asset.status = MediaStatus.UPLOADED.name
		asset.updatedAt = Instant.now(clock)
	}

	@Transactional(readOnly = true)
	fun loadBytes(mediaId: UUID): StoredMedia {
		val asset = assets.findByIdAndDeletedAtIsNull(mediaId)
			?: throw ResourceNotFoundException("Media not found.")
		val bytes = loadPayload(asset) ?: throw MediaNotReadyException()
		return StoredMedia(asset.contentType, bytes)
	}

	@Transactional
	fun confirm(userId: UUID, mediaId: UUID, request: ConfirmMediaUploadRequest): MediaAssetDto {
		val asset = requireOwned(userId, mediaId)
		if (request.uploaded) {
			if (!hasPayload(asset)) {
				throw MediaNotReadyException()
			}
			asset.status = MediaStatus.READY.name
			asset.updatedAt = Instant.now(clock)
		}
		return toDto(asset)
	}

	@Transactional(readOnly = true)
	fun get(userId: UUID, mediaId: UUID): MediaAssetDto = toDto(requireOwned(userId, mediaId))

	@Transactional(readOnly = true)
	fun publicUrlIfReady(userId: UUID, mediaId: UUID): String? {
		val asset = assets.findByIdAndDeletedAtIsNull(mediaId) ?: return null
		if (asset.userId != userId || asset.status != MediaStatus.READY.name) {
			return null
		}
		return "${publicBaseUrl()}/dev-uploads/${asset.id}"
	}

	@Transactional(readOnly = true)
	fun ocr(userId: UUID, mediaId: UUID): ReceiptOcrResultDto {
		val asset = requireOwned(userId, mediaId)
		if (asset.status != MediaStatus.READY.name) {
			throw MediaNotReadyException()
		}
		val bytes = loadPayload(asset) ?: throw MediaNotReadyException()
		return ocr.read(asset.id, asset.purpose, asset.contentType, bytes)
	}

	private fun hasPayload(asset: MediaAsset): Boolean {
		val key = asset.storageKey
		if (key != null && blobs.exists(key)) {
			return true
		}
		return asset.content != null
	}

	private fun loadPayload(asset: MediaAsset): ByteArray? {
		val key = asset.storageKey
		if (key != null) {
			blobs.get(key)?.let { return it }
		}
		return asset.content
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

	private fun sha256Hex(body: ByteArray): String =
		MessageDigest.getInstance("SHA-256").digest(body).joinToString("") { byte -> "%02x".format(byte) }

	companion object {
		const val MAX_BYTES = 12L * 1024 * 1024

		fun keyFor(asset: MediaAsset): String = "media/${asset.userId}/${asset.id}"
	}
}

data class StoredMedia(
	val contentType: String,
	val bytes: ByteArray,
)
