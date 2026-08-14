package com.shoptourr.export

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ExportPdfTest {

	@Test
	fun `bytes are a PDF that contains the title and body`() {
		val pdf = ExportPdf.render("Lisbon, Portugal", "Coffee, large\n4.50 EUR")
		val text = pdf.toString(Charsets.ISO_8859_1)

		assertTrue(text.startsWith("%PDF-1.4"), text.take(20))
		assertTrue(text.contains("%%EOF"), text.takeLast(40))
		assertTrue(text.contains("/Encoding /Identity-H"), text)
		assertTrue(text.contains("/BaseFont /DejaVuSans"), text)
		assertTrue(text.contains("<004C>"), text)
		assertTrue(text.contains("<006C>"), text)
	}

	@Test
	fun `cyrillic is mapped in ToUnicode instead of question marks`() {
		val pdf = ExportPdf.render("Лиссабон", "Привет")
		val text = pdf.toString(Charsets.ISO_8859_1)

		assertTrue(text.contains("<041B>"), text)
		assertTrue(text.contains("<041F>"), text)
		assertTrue(!text.contains("??????"), text)
	}

	@Test
	fun `wraps long lines`() {
		assertEquals(listOf("abcdef", "gh"), ExportPdf.wrap("abcdefgh", 6))
	}
}

class EmbeddedTtfTest {

	@Test
	fun `cmap resolves latin and cyrillic`() {
		val ttf = EmbeddedTtf.load()

		assertNotEquals(0, ttf.gid('A'.code))
		assertNotEquals(0, ttf.gid('П'.code))
		assertTrue(ttf.width1000(ttf.gid('A'.code)) > 0)
	}
}
