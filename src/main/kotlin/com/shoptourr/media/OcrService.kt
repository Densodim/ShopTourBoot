package com.shoptourr.media

import com.shoptourr.config.OcrProperties
import com.shoptourr.media.dto.ReceiptOcrResultDto
import org.slf4j.LoggerFactory
import org.springframework.data.redis.RedisConnectionFailureException
import org.springframework.data.redis.RedisSystemException
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import java.security.MessageDigest
import java.util.UUID

@Service
class OcrService(
	private val live: LiveOcrClient,
	private val redis: StringRedisTemplate,
	private val properties: OcrProperties,
) {

	private val log = LoggerFactory.getLogger(javaClass)

	fun read(mediaId: UUID, purpose: String, contentType: String, bytes: ByteArray): ReceiptOcrResultDto {
		val local = ReceiptOcr.parse(mediaId, purpose, contentType, bytes)
		if (local.confidence >= STRUCTURED_CONFIDENCE) {
			return local
		}
		if (purpose != "RECEIPT") {
			return local
		}
		liveText(contentType, bytes)?.let { text ->
			return ReceiptOcr.parseOcrText(mediaId, text)
		}
		return local
	}

	private fun liveText(contentType: String, bytes: ByteArray): String? {
		if (!properties.enabled) {
			return null
		}
		val digest = sha256Hex(bytes)
		cached(digest)?.let { return it }
		val text = live.read(contentType, bytes) ?: return null
		writeCache(digest, text)
		return text
	}

	private fun cached(digest: String): String? {
		return try {
			redis.opsForValue().get(cacheKey(digest))?.takeIf { it.isNotBlank() }
		} catch (ex: RedisConnectionFailureException) {
			log.warn("OCR cache unavailable", ex)
			null
		} catch (ex: RedisSystemException) {
			log.warn("OCR cache unavailable", ex)
			null
		}
	}

	private fun writeCache(digest: String, text: String) {
		try {
			redis.opsForValue().set(cacheKey(digest), text, properties.cacheTtl)
		} catch (ex: RedisConnectionFailureException) {
			log.warn("OCR cache write failed", ex)
		} catch (ex: RedisSystemException) {
			log.warn("OCR cache write failed", ex)
		}
	}

	companion object {
		const val STRUCTURED_CONFIDENCE = 0.85
		private const val LIVE_PREFIX = "ocr:live:"

		fun cacheKey(digest: String): String = "$LIVE_PREFIX$digest"

		internal fun sha256Hex(body: ByteArray): String =
			MessageDigest.getInstance("SHA-256").digest(body).joinToString("") { byte -> "%02x".format(byte) }
	}
}
