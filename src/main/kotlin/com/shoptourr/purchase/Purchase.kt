package com.shoptourr.purchase

import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

@Entity
@Table(name = "purchase")
class Purchase(
	@Id
	val id: UUID = UUID.randomUUID(),

	@Column(name = "trip_id", nullable = false)
	val tripId: UUID,

	@Column(nullable = false, length = 200)
	var name: String,

	@Column(nullable = false, length = 32)
	var category: String,

	@Column(name = "gross_amount", nullable = false, precision = 19, scale = 4)
	var grossAmount: BigDecimal,

	@Column(nullable = false, length = 3)
	var currency: String,

	@Column(name = "net_amount", nullable = false, precision = 19, scale = 4)
	var netAmount: BigDecimal,

	@Column(name = "vat_amount", nullable = false, precision = 19, scale = 4)
	var vatAmount: BigDecimal,

	@Column(name = "vat_rate_percent", nullable = false, precision = 5, scale = 2)
	var vatRatePercent: BigDecimal,

	@Column(name = "vat_included", nullable = false)
	var vatIncluded: Boolean,

	@Column(name = "tax_refund_eligible", nullable = false)
	var taxRefundEligible: Boolean = false,

	@Column(length = 200)
	var place: String? = null,

	@Column(name = "purchase_date", nullable = false)
	var purchaseDate: LocalDate,

	@Column(name = "purchase_time", nullable = false)
	var purchaseTime: LocalTime,

	@Column(name = "receipt_media_id")
	var receiptMediaId: UUID? = null,

	@Column(name = "created_at", nullable = false)
	val createdAt: Instant,

	@Column(name = "updated_at", nullable = false)
	var updatedAt: Instant,

	@Column(name = "deleted_at")
	var deletedAt: Instant? = null,

	@ElementCollection(fetch = FetchType.LAZY)
	@CollectionTable(name = "purchase_split", joinColumns = [JoinColumn(name = "purchase_id")])
	@Column(name = "traveler_id", nullable = false)
	val splitTravelerIds: MutableSet<UUID> = mutableSetOf(),
)
