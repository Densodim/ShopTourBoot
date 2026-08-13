package com.shoptourr.media.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

enum class MediaPurpose {
	RECEIPT, AVATAR, DIARY, EXPORT
}

enum class MediaStatus {
	PENDING_UPLOAD, UPLOADED, PROCESSING, READY, FAILED
}

data class CreateMediaUploadIntentRequest(
	@field:NotNull
	val purpose: MediaPurpose,
	@field:NotBlank
	@field:Size(max = 128)
	val contentType: String,
	@field:Positive
	val byteSize: Long,
	@field:Size(min = 64, max = 64)
	val sha256Hex: String? = null,
)

data class MediaUploadIntentResponse(
	val mediaId: UUID,
	val uploadUrl: String,
	val requiredHeaders: Map<String, String>,
	val uploadExpiresAt: Instant,
	val status: MediaStatus,
)

data class ConfirmMediaUploadRequest(
	val uploaded: Boolean = true,
)

data class MediaAssetDto(
	val id: UUID,
	val purpose: MediaPurpose,
	val status: MediaStatus,
	val contentType: String,
	val byteSize: Long,
	val downloadUrl: String?,
	val thumbnailUrl: String?,
	val createdAt: Instant,
)

data class ReceiptOcrResultDto(
	val mediaId: UUID,
	val suggestedName: String?,
	val suggestedAmount: String?,
	val suggestedPlace: String?,
	val suggestedCategory: String?,
	val confidence: Double,
)
