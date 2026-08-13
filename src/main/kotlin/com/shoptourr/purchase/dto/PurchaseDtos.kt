package com.shoptourr.purchase.dto

import com.shoptourr.shared.dto.MoneyDto
import com.shoptourr.shared.dto.VatBreakdownDto
import jakarta.validation.Valid
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

enum class PurchaseCategory {
	FOOD, TRANSPORT, SOUVENIRS, HOTEL, CULTURE, OTHER
}

data class SplitShareDto(
	val travelerId: UUID,
	val travelerName: String,
	val share: MoneyDto,
)

data class PurchaseDto(
	val id: UUID,
	val tripId: UUID,
	val name: String,
	val category: PurchaseCategory,
	val amount: MoneyDto,
	val vat: VatBreakdownDto,
	val taxRefundEligible: Boolean,
	val place: String?,
	val purchaseDate: LocalDate,
	val purchaseTime: LocalTime,
	val receiptMediaId: UUID?,
	val receiptThumbnailUrl: String?,
	val splitWithTravelerIds: List<UUID>,
	val splits: List<SplitShareDto>,
	val yourShare: MoneyDto,
	val quoteEquivalent: MoneyDto?,
	val createdAt: Instant,
	val updatedAt: Instant,
)

data class CreatePurchaseRequest(
	@field:NotBlank
	@field:Size(min = 1, max = 200)
	val name: String,
	@field:NotNull
	val category: PurchaseCategory,
	@field:NotNull
	@field:Valid
	val amount: MoneyDto,
	val vatIncluded: Boolean = true,
	@field:DecimalMin("0.0")
	@field:DecimalMax("100.0")
	val vatRatePercent: BigDecimal? = null,
	val taxRefundEligible: Boolean = false,
	@field:Size(max = 200)
	val place: String? = null,
	val purchaseDate: LocalDate? = null,
	val purchaseTime: LocalTime? = null,
	val receiptMediaId: UUID? = null,
	val splitWithTravelerIds: List<UUID>? = null,
)

data class UpdatePurchaseRequest(
	@field:Size(min = 1, max = 200)
	val name: String? = null,
	val category: PurchaseCategory? = null,
	@field:Valid
	val amount: MoneyDto? = null,
	val vatIncluded: Boolean? = null,
	@field:DecimalMin("0.0")
	@field:DecimalMax("100.0")
	val vatRatePercent: BigDecimal? = null,
	val taxRefundEligible: Boolean? = null,
	@field:Size(max = 200)
	val place: String? = null,
	val purchaseDate: LocalDate? = null,
	val purchaseTime: LocalTime? = null,
	val receiptMediaId: UUID? = null,
	val splitWithTravelerIds: List<UUID>? = null,
)

data class PurchaseDayGroupDto(
	val date: LocalDate,
	val labelKey: String,
	val dayTotal: MoneyDto,
	val items: List<PurchaseDto>,
)

data class TripPurchasesResponse(
	val spentTotal: MoneyDto,
	val budget: MoneyDto,
	val remaining: MoneyDto,
	val days: List<PurchaseDayGroupDto>,
)
