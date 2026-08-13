package com.shoptourr.trip

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Entity
@Table(name = "trip")
class Trip(
	@Id
	val id: UUID = UUID.randomUUID(),

	@Column(name = "owner_id", nullable = false)
	val ownerId: UUID,

	@Column(nullable = false, length = 120)
	var city: String,

	@Column(nullable = false, length = 120)
	var country: String,

	@Column(name = "country_code", length = 2)
	var countryCode: String? = null,

	@Column(name = "flag_emoji", length = 16)
	var flagEmoji: String? = null,

	@Column(nullable = false, length = 16)
	var status: String,

	@Column(name = "start_date", nullable = false)
	var startDate: LocalDate,

	@Column(name = "end_date", nullable = false)
	var endDate: LocalDate,

	@Column(name = "budget_amount", nullable = false, precision = 19, scale = 4)
	var budgetAmount: BigDecimal,

	@Column(name = "budget_currency", nullable = false, length = 3)
	var budgetCurrency: String,

	@Column(name = "default_vat_rate_percent", nullable = false, precision = 5, scale = 2)
	var defaultVatRatePercent: BigDecimal = BigDecimal.ZERO,

	@Column(name = "fx_trip_currency", length = 3)
	var fxTripCurrency: String? = null,

	@Column(name = "fx_quote_currency", length = 3)
	var fxQuoteCurrency: String? = null,

	@Column(name = "fx_rate", precision = 19, scale = 8)
	var fxRate: BigDecimal? = null,

	@Column(name = "fx_rate_date")
	var fxRateDate: LocalDate? = null,

	@Column(name = "fx_provider", length = 64)
	var fxProvider: String? = null,

	@Column(name = "created_at", nullable = false)
	val createdAt: Instant,

	@Column(name = "updated_at", nullable = false)
	var updatedAt: Instant,

	@Column(name = "deleted_at")
	var deletedAt: Instant? = null,

	@OneToMany(mappedBy = "trip", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
	val travelers: MutableList<Traveler> = mutableListOf(),
)
