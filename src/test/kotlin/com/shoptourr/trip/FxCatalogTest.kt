package com.shoptourr.trip

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class FxCatalogTest {

	@Test
	fun `same currency is identity`() {
		val quote = FxCatalog.quote("eur", "EUR")

		assertEquals(0, quote.rate.compareTo(BigDecimal.ONE))
		assertEquals(FxCatalog.IDENTITY_PROVIDER, quote.provider)
	}

	@Test
	fun `eur to rub uses the catalog cross`() {
		val quote = FxCatalog.quote("EUR", "RUB")

		assertEquals(0, quote.rate.compareTo(BigDecimal("95.000000")))
		assertEquals(FxCatalog.PROVIDER, quote.provider)
	}

	@Test
	fun `usd to eur inverts through the euro table`() {
		val quote = FxCatalog.quote("USD", "EUR")

		assertEquals(0, quote.rate.compareTo(BigDecimal("1").divide(BigDecimal("1.08"), 6, java.math.RoundingMode.HALF_UP)))
		assertEquals(FxCatalog.PROVIDER, quote.provider)
	}

	@Test
	fun `unknown currency stays a stub one-to-one`() {
		val quote = FxCatalog.quote("EUR", "XXX")

		assertEquals(0, quote.rate.compareTo(BigDecimal.ONE))
		assertEquals(FxCatalog.UNKNOWN_PROVIDER, quote.provider)
	}
}
