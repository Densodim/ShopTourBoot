package com.shoptourr.media

import com.shoptourr.config.OcrProperties
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.ValueOperations
import java.time.Duration
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class OcrServiceTest {

	@Mock
	private lateinit var live: LiveOcrClient

	@Mock
	private lateinit var redis: StringRedisTemplate

	@Mock
	private lateinit var values: ValueOperations<String, String>

	private lateinit var service: OcrService
	private val mediaId = UUID.fromString("33333333-3333-3333-3333-333333333333")

	@BeforeEach
	fun setUp() {
		org.mockito.Mockito.lenient().`when`(redis.opsForValue()).thenReturn(values)
		service = OcrService(live, redis, OcrProperties(apiKey = "test-key"))
	}

	@Test
	fun `structured text skips the live client`() {
		val body = "name: Coffee\namount: 4.50\ncategory: FOOD".toByteArray()

		val result = service.read(mediaId, "RECEIPT", "text/plain", body)

		assertEquals("Coffee", result.suggestedName)
		assertEquals("4.50", result.suggestedAmount)
		verify(live, never()).read("text/plain", body)
	}

	@Test
	fun `a live hit is preferred over the jpeg stub`() {
		val jpeg = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 1, 2, 3)
		val digest = OcrService.sha256Hex(jpeg)
		`when`(values.get("ocr:live:$digest")).thenReturn(null)
		`when`(live.read("image/jpeg", jpeg)).thenReturn("Time Out Market\nLisbon\nTotal 12.50 EUR")

		val result = service.read(mediaId, "RECEIPT", "image/jpeg", jpeg)

		assertEquals("Time Out Market", result.suggestedName)
		assertEquals("12.50", result.suggestedAmount)
		assertEquals("FOOD", result.suggestedCategory)
		assertEquals(0.7, result.confidence)
		verify(values).set("ocr:live:$digest", "Time Out Market\nLisbon\nTotal 12.50 EUR", Duration.ofDays(7))
	}

	@Test
	fun `cache skips the live client`() {
		val jpeg = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 1, 2, 3)
		`when`(values.get("ocr:live:${OcrService.sha256Hex(jpeg)}")).thenReturn("Cafe\nTotal 3.00")

		val result = service.read(mediaId, "RECEIPT", "image/jpeg", jpeg)

		assertEquals("Cafe", result.suggestedName)
		assertEquals("3.00", result.suggestedAmount)
		verify(live, never()).read("image/jpeg", jpeg)
	}

	@Test
	fun `live miss keeps the jpeg stub`() {
		val jpeg = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 1, 2, 3)
		`when`(values.get("ocr:live:${OcrService.sha256Hex(jpeg)}")).thenReturn(null)
		`when`(live.read("image/jpeg", jpeg)).thenReturn(null)

		val result = service.read(mediaId, "RECEIPT", "image/jpeg", jpeg)

		assertEquals("Receipt", result.suggestedName)
		assertEquals(0.1, result.confidence)
	}
}
