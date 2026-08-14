package com.shoptourr.media

import com.shoptourr.media.dto.MediaPurpose
import com.shoptourr.media.dto.ReceiptOcrResultDto
import com.shoptourr.purchase.dto.PurchaseCategory
import java.math.RoundingMode
import java.util.UUID

/**
 * Local receipt parser. Structured UTF-8 `key: value` bodies are parsed directly;
 * free-form OCR text is reduced to name / amount / place / category. Image bytes
 * without a live hit stay a low-confidence placeholder.
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

	fun parseOcrText(mediaId: UUID, text: String): ReceiptOcrResultDto {
		val lines = text.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.toList()
		val amount = findAmount(lines)
		val name = lines.firstOrNull { looksLikeMerchant(it) }?.take(80) ?: "Receipt"
		val place = lines.asSequence()
			.drop(1)
			.firstOrNull { looksLikeMerchant(it) && it != name }
			?.take(80)
		return ReceiptOcrResultDto(
			mediaId = mediaId,
			suggestedName = name,
			suggestedAmount = amount ?: "0.00",
			suggestedPlace = place,
			suggestedCategory = inferCategory(text),
			confidence = if (amount != null) 0.7 else 0.45,
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

	private fun findAmount(lines: List<String>): String? {
		val fromTotal = lines.filter { TOTAL.containsMatchIn(it) }
			.mapNotNull { line -> MONEY.findAll(line).lastOrNull()?.groupValues?.get(1) }
			.mapNotNull { normalizeAmount(it) }
			.lastOrNull()
		if (fromTotal != null) {
			return fromTotal
		}
		return lines.flatMap { line -> MONEY.findAll(line).map { it.groupValues[1] } }
			.mapNotNull { normalizeAmount(it) }
			.maxByOrNull { it.toBigDecimal() }
	}

	private fun looksLikeMerchant(line: String): Boolean {
		if (line.length < 3 || SKIP_LINE.containsMatchIn(line)) {
			return false
		}
		return line.any { it.isLetter() }
	}

	private fun inferCategory(text: String): String {
		val lower = text.lowercase()
		return when {
			FOOD_HINTS.any { it in lower } -> PurchaseCategory.FOOD.name
			TRANSPORT_HINTS.any { it in lower } -> PurchaseCategory.TRANSPORT.name
			HOTEL_HINTS.any { it in lower } -> PurchaseCategory.HOTEL.name
			CULTURE_HINTS.any { it in lower } -> PurchaseCategory.CULTURE.name
			SOUVENIR_HINTS.any { it in lower } -> PurchaseCategory.SOUVENIRS.name
			else -> PurchaseCategory.OTHER.name
		}
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
	private val MONEY = Regex("""(?:[€$£]\s*)?(\d{1,5}(?:[.,]\d{2}))(?:\s*(?:€|eur|usd|gbp|\$))?""", RegexOption.IGNORE_CASE)
	private val TOTAL = Regex("""total|summe|suma|montant|amount|to pay|visa|mastercard""", RegexOption.IGNORE_CASE)
	private val SKIP_LINE = Regex("""^(total|sum|subtotal|vat|tax|cash|card|change)\b""", RegexOption.IGNORE_CASE)
	private val FOOD_HINTS = listOf("restaurant", "cafe", "coffee", "bakery", "market", "grocery", "pizza", "lunch", "dinner")
	private val TRANSPORT_HINTS = listOf("uber", "taxi", "metro", "train", "bus", "flight", "airline")
	private val HOTEL_HINTS = listOf("hotel", "hostel", "airbnb")
	private val CULTURE_HINTS = listOf("museum", "theatre", "theater", "ticket", "gallery")
	private val SOUVENIR_HINTS = listOf("souvenir", "gift shop", "zara", "uniqlo")
}
