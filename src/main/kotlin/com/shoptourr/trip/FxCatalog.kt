package com.shoptourr.trip

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Fallback FX table when the live feed is down or missing a pair.
 * Rates are units of [quote] per 1 unit of [trip] currency, derived from a EUR cross.
 */
object FxCatalog {

	const val PROVIDER = "catalog"
	const val IDENTITY_PROVIDER = "identity"
	const val UNKNOWN_PROVIDER = "stub"

	data class Quote(
		val rate: BigDecimal,
		val provider: String,
	)

	fun quote(tripCurrency: String, quoteCurrency: String): Quote {
		val from = tripCurrency.uppercase()
		val to = quoteCurrency.uppercase()
		if (from == to) {
			return Quote(BigDecimal.ONE.setScale(SCALE), IDENTITY_PROVIDER)
		}
		val fromPerEuro = PER_EURO[from]
		val toPerEuro = PER_EURO[to]
		if (fromPerEuro == null || toPerEuro == null) {
			return Quote(BigDecimal.ONE.setScale(SCALE), UNKNOWN_PROVIDER)
		}
		return Quote(toPerEuro.divide(fromPerEuro, SCALE, RoundingMode.HALF_UP), PROVIDER)
	}

	private const val SCALE = 6

	private val PER_EURO: Map<String, BigDecimal> = mapOf(
		"EUR" to bd("1"),
		"USD" to bd("1.08"),
		"GBP" to bd("0.85"),
		"JPY" to bd("160"),
		"NOK" to bd("11.50"),
		"SEK" to bd("11.20"),
		"DKK" to bd("7.46"),
		"CHF" to bd("0.96"),
		"PLN" to bd("4.30"),
		"CZK" to bd("25.00"),
		"HUF" to bd("390"),
		"RUB" to bd("95.00"),
		"TRY" to bd("36.00"),
		"BRL" to bd("6.10"),
		"CAD" to bd("1.48"),
		"AUD" to bd("1.65"),
		"CNY" to bd("7.80"),
		"KRW" to bd("1480"),
		"INR" to bd("90.00"),
	)

	private fun bd(value: String): BigDecimal = BigDecimal(value)
}
