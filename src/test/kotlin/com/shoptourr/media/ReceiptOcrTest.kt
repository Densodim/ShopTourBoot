package com.shoptourr.media

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.util.UUID

class ReceiptOcrTest {

	private val mediaId = UUID.fromString("33333333-3333-3333-3333-333333333333")

	@Test
	fun `parses key-value UTF-8 receipt bytes`() {
		val body = """
			name: Coffee, large
			amount: 4,50
			place: Cafe A Brasileira
			category: FOOD
		""".trimIndent().toByteArray()

		val result = ReceiptOcr.parse(mediaId, "RECEIPT", "text/plain", body)

		assertEquals("Coffee, large", result.suggestedName)
		assertEquals("4.50", result.suggestedAmount)
		assertEquals("Cafe A Brasileira", result.suggestedPlace)
		assertEquals("FOOD", result.suggestedCategory)
		assertEquals(0.85, result.confidence)
	}

	@Test
	fun `binary images stay a low-confidence stub`() {
		val result = ReceiptOcr.parse(mediaId, "RECEIPT", "image/jpeg", byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 1, 2, 3))

		assertEquals("Receipt", result.suggestedName)
		assertEquals("0.00", result.suggestedAmount)
		assertNull(result.suggestedPlace)
		assertEquals("OTHER", result.suggestedCategory)
		assertEquals(0.1, result.confidence)
	}

	@Test
	fun `ocr text yields merchant total and category`() {
		val result = ReceiptOcr.parseOcrText(
			mediaId,
			"""
			Time Out Market
			Lisbon
			VAT 23%
			Total €12,50
			""".trimIndent(),
		)

		assertEquals("Time Out Market", result.suggestedName)
		assertEquals("12.50", result.suggestedAmount)
		assertEquals("Lisbon", result.suggestedPlace)
		assertEquals("FOOD", result.suggestedCategory)
		assertEquals(0.7, result.confidence)
	}

	@Test
	fun `non-receipt purpose has no suggestions`() {
		val result = ReceiptOcr.parse(mediaId, "AVATAR", "text/plain", "name: X".toByteArray())

		assertNull(result.suggestedName)
		assertEquals(0.0, result.confidence)
	}
}
