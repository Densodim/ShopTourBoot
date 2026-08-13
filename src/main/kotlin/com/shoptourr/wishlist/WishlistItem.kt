package com.shoptourr.wishlist

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "wishlist_item")
class WishlistItem(
	@Id
	val id: UUID = UUID.randomUUID(),

	@Column(name = "user_id", nullable = false)
	val userId: UUID,

	@Column(nullable = false, length = 200)
	var name: String,

	@Column(nullable = false, length = 120)
	var city: String,

	@Column(name = "target_amount", nullable = false, precision = 19, scale = 4)
	var targetAmount: BigDecimal,

	@Column(name = "target_currency", nullable = false, length = 3)
	var targetCurrency: String,

	@Column(name = "icon_emoji", length = 8)
	var iconEmoji: String? = null,

	@Column(length = 500)
	var note: String? = null,

	@Column(name = "created_at", nullable = false)
	val createdAt: Instant,

	@Column(name = "updated_at", nullable = false)
	var updatedAt: Instant,

	@Column(name = "deleted_at")
	var deletedAt: Instant? = null,
)
