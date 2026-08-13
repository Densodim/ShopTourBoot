package com.shoptourr.trip.dto

import com.fasterxml.jackson.annotation.JsonProperty
import com.shoptourr.shared.dto.ExchangeRateDto
import com.shoptourr.shared.dto.MoneyDto
import jakarta.validation.Valid
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

enum class TripStatus {
	UPCOMING, ACTIVE, PAST, ARCHIVED
}

data class TravelerDto(
	val id: UUID,
	val name: String,
	val colorHex: String,
	val avatarGlyph: String?,
	@get:JsonProperty("isOwner")
	val isOwner: Boolean,
)

data class CreateTravelerRequest(
	@field:NotBlank
	@field:Size(min = 1, max = 60)
	val name: String,
	@field:NotBlank
	@field:Pattern(regexp = "^#[0-9A-Fa-f]{6}$")
	val colorHex: String,
	@field:Size(min = 1, max = 2)
	val avatarGlyph: String? = null,
)

data class TripDto(
	val id: UUID,
	val city: String,
	val country: String,
	val countryCode: String?,
	val flagEmoji: String?,
	val status: TripStatus,
	val startDate: LocalDate,
	val endDate: LocalDate,
	val datesLabel: String?,
	val budget: MoneyDto,
	val spent: MoneyDto,
	val remaining: MoneyDto,
	val purchaseCount: Int,
	val dayCount: Int,
	val currentDayNumber: Int?,
	val defaultVatRatePercent: BigDecimal,
	val exchangeRate: ExchangeRateDto?,
	val travelers: List<TravelerDto>,
	val createdAt: Instant,
	val updatedAt: Instant,
)

data class TripSummaryDto(
	val id: UUID,
	val city: String,
	val country: String,
	val flagEmoji: String?,
	val status: TripStatus,
	val startDate: LocalDate,
	val endDate: LocalDate,
	val datesLabel: String?,
	val budget: MoneyDto,
	val spent: MoneyDto,
	val purchaseCount: Int,
	val currentDayNumber: Int?,
	val dayCount: Int?,
)

data class CreateTripRequest(
	@field:NotBlank
	@field:Size(min = 1, max = 120)
	val city: String,
	@field:NotBlank
	@field:Size(min = 1, max = 120)
	val country: String,
	@field:Size(min = 2, max = 2)
	val countryCode: String? = null,
	@field:NotNull
	val startDate: LocalDate,
	@field:NotNull
	val endDate: LocalDate,
	@field:NotNull
	@field:Valid
	val budget: MoneyDto,
	@field:DecimalMin("0.0")
	@field:DecimalMax("100.0")
	val defaultVatRatePercent: BigDecimal? = null,
	@field:Size(min = 3, max = 3)
	val quoteCurrency: String? = null,
	val travelers: List<@Valid CreateTravelerRequest>? = null,
)

data class UpdateTripRequest(
	@field:Size(min = 1, max = 120)
	val city: String? = null,
	@field:Size(min = 1, max = 120)
	val country: String? = null,
	@field:Size(min = 2, max = 2)
	val countryCode: String? = null,
	val startDate: LocalDate? = null,
	val endDate: LocalDate? = null,
	@field:Valid
	val budget: MoneyDto? = null,
	@field:DecimalMin("0.0")
	@field:DecimalMax("100.0")
	val defaultVatRatePercent: BigDecimal? = null,
	val status: TripStatus? = null,
)

data class TripListResponse(
	val active: List<TripSummaryDto>,
	val upcoming: List<TripSummaryDto>,
	val past: List<TripSummaryDto>,
)

enum class TripInviteStatus {
	PENDING, ACCEPTED, DECLINED, EXPIRED
}

data class InviteTravelerRequest(
	@field:NotBlank
	@field:Size(max = 254)
	val email: String,
	@field:Size(min = 1, max = 60)
	val displayNameHint: String? = null,
)

data class TripInviteDto(
	val id: UUID,
	val tripId: UUID,
	val email: String,
	val status: TripInviteStatus,
	val createdAt: Instant,
	val expiresAt: Instant?,
)
