package com.shoptourr.shared.dto

import com.shoptourr.shared.validation.FieldPatterns
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.math.BigDecimal

data class MoneyDto(
	@field:NotNull
	@field:DecimalMin(value = "0.00", inclusive = true)
	val amount: BigDecimal,
	@field:NotBlank
	@field:Size(min = 3, max = 3)
	@field:Pattern(regexp = FieldPatterns.ISO_4217)
	val currency: String,
)

data class ExchangeRateDto(
	val tripCurrency: String,
	val quoteCurrency: String,
	val rate: BigDecimal,
	val rateDate: String,
	val provider: String? = null,
)

data class VatBreakdownDto(
	val net: BigDecimal,
	val vat: BigDecimal,
	val gross: BigDecimal,
	val vatRatePercent: BigDecimal,
	val vatIncluded: Boolean,
)
