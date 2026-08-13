package com.shoptourr.purchase

import java.math.BigDecimal
import java.math.RoundingMode

object VatCalculator {

	fun breakdown(amount: BigDecimal, ratePercent: BigDecimal, vatIncluded: Boolean): Result {
		val rate = ratePercent.divide(BigDecimal("100"), 8, RoundingMode.HALF_UP)
		val (net, vat, gross) = if (vatIncluded) {
			val gross = amount
			val net = gross.divide(BigDecimal.ONE.add(rate), 4, RoundingMode.HALF_UP)
			Triple(net, gross.subtract(net), gross)
		} else {
			val net = amount
			val vat = net.multiply(rate).setScale(4, RoundingMode.HALF_UP)
			Triple(net, vat, net.add(vat))
		}
		return Result(
			net = net.setScale(2, RoundingMode.HALF_UP),
			vat = vat.setScale(2, RoundingMode.HALF_UP),
			gross = gross.setScale(2, RoundingMode.HALF_UP),
			vatRatePercent = ratePercent,
			vatIncluded = vatIncluded,
		)
	}

	data class Result(
		val net: BigDecimal,
		val vat: BigDecimal,
		val gross: BigDecimal,
		val vatRatePercent: BigDecimal,
		val vatIncluded: Boolean,
	)
}
