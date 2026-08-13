package com.shoptourr.purchase

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class VatCalculatorTest {

	@Test
	fun `gross-inclusive 23 percent splits net and vat`() {
		val result = VatCalculator.breakdown(BigDecimal("123.00"), BigDecimal("23"), vatIncluded = true)

		assertEquals(BigDecimal("100.00"), result.net)
		assertEquals(BigDecimal("23.00"), result.vat)
		assertEquals(BigDecimal("123.00"), result.gross)
	}

	@Test
	fun `net-exclusive 23 percent adds vat on top`() {
		val result = VatCalculator.breakdown(BigDecimal("100.00"), BigDecimal("23"), vatIncluded = false)

		assertEquals(BigDecimal("100.00"), result.net)
		assertEquals(BigDecimal("23.00"), result.vat)
		assertEquals(BigDecimal("123.00"), result.gross)
	}
}
