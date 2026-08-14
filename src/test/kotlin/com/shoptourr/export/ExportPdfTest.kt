package com.shoptourr.export

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ExportPdfTest {

	@Test
	fun `bytes are a PDF that contains the title and body`() {
		val pdf = ExportPdf.render("Lisbon, Portugal", "Coffee, large\n4.50 EUR")
		val text = pdf.toString(Charsets.ISO_8859_1)

		assertTrue(text.startsWith("%PDF-1.4"), text.take(20))
		assertTrue(text.contains("%%EOF"), text.takeLast(40))
		assertTrue(text.contains("(Lisbon, Portugal)"), text)
		assertTrue(text.contains("(Coffee, large)"), text)
		assertTrue(text.contains("/BaseFont /Courier"), text)
	}

	@Test
	fun `escapes parentheses in Tj strings`() {
		assertEquals("Cafe \\(Chiado\\)", ExportPdf.escape("Cafe (Chiado)"))
	}

	@Test
	fun `wraps long lines`() {
		assertEquals(listOf("abcdef", "gh"), ExportPdf.wrap("abcdefgh", 6))
	}
}
