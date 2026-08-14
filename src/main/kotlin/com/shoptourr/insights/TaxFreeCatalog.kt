package com.shoptourr.insights

import java.math.BigDecimal

/**
 * Country tax-free minima and estimated refund rates. Shared by insights and CSV export.
 */
internal object TaxFreeCatalog {

	data class Rules(val minimum: BigDecimal, val rate: BigDecimal)

	fun rules(countryCode: String?): Rules = when (countryCode?.uppercase()) {
		"PT" -> Rules(BigDecimal("50.00"), BigDecimal("0.13"))
		"JP" -> Rules(BigDecimal("5000.00"), BigDecimal("0.10"))
		else -> Rules(BigDecimal("100.00"), BigDecimal("0.10"))
	}
}
