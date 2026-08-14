package com.shoptourr.media

import com.shoptourr.media.dto.MediaPurpose
import com.shoptourr.media.dto.ReceiptOcrResultDto
import com.shoptourr.purchase.dto.PurchaseCategory
import java.math.RoundingMode
import java.util.UUID

/**
 * Local stand-in for a receipt OCR provider. Structured UTF-8 `key: value` bodies are parsed;
 * image bytes keep a low-confidence placeholder until a real engine is wired.
 */
internal object ReceiptOcr {

	fun parse(mediaId: UUID, purpose: String, contentType: String, bytes: ByteArray): ReceiptOcrResultDto {
		if (purpose != MediaPurpose.RECEIPT.name) {
			return ReceiptOcrResultDto(mediaId, null, null, null, null, 0.0)
		}
		val fields = extractFields(contentType, bytes) ?: return binaryStub(mediaId)
		return ReceiptOcrResultDto(
			mediaId = mediaId,
			suggestedName = fields["name"]?.takeIf { it.isNotBlank() } ?: "Receipt",
			suggestedAmount = normalizeAmount(fields["amount"]) ?: "0.00",
			suggestedPlace = fields["place"]?.takeIf { it.isNotBlank() },
			suggestedCategory = categoryOf(fields["category"]),
			confidence = 0.85,
		)
	}

	private fun extractFields(contentType: String, bytes: ByteArray): Map<String, String>? {
		if (!looksLikeText(contentType, bytes)) {
			return null
		}
		val text = bytes.toString(Charsets.UTF_8).trimStart('\uFEFF')
		val fields = linkedMapOf<String, String>()
		text.lineSequence().forEach { line ->
			val match = FIELD.matchEntire(line.trim()) ?: return@forEach
			fields[match.groupValues[1].lowercase()] = match.groupValues[2].trim()
		}
		return fields.takeIf { it.isNotEmpty() }
	}

	private fun looksLikeText(contentType: String, bytes: ByteArray): Boolean {
		if (contentType.startsWith("text/", ignoreCase = true)) {
			return true
		}
		if (bytes.isEmpty() || bytes.contains(0)) {
			return false
		}
		val sample = bytes.decodeToString()
		return sample.contains(':') || sample.contains('=')
	}

	private fun normalizeAmount(raw: String?): String? {
		if (raw.isNullOrBlank()) {
			return null
		}
		val numeric = raw.replace(',', '.').filter { it.isDigit() || it == '.' }
		val amount = numeric.toBigDecimalOrNull() ?: return null
		return amount.setScale(2, RoundingMode.HALF_UP).toPlainString()
	}

	private fun categoryOf(raw: String?): String {
		val value = raw?.uppercase()?.trim().orEmpty()
		return PurchaseCategory.entries.firstOrNull { it.name == value }?.name ?: PurchaseCategory.OTHER.name
	}

	private fun binaryStub(mediaId: UUID) = ReceiptOcrResultDto(
		mediaId = mediaId,
		suggestedName = "Receipt",
		suggestedAmount = "0.00",
		suggestedPlace = null,
		suggestedCategory = PurchaseCategory.OTHER.name,
		confidence = 0.1,
	)

	private val FIELD = Regex("^([A-Za-z][A-Za-z0-9_]*)\\s*[:=]\\s*(.+)$")
}
