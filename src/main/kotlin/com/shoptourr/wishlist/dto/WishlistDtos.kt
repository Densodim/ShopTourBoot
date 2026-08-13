package com.shoptourr.wishlist.dto

import com.shoptourr.shared.dto.MoneyDto
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

data class WishlistItemDto(
	val id: UUID,
	val name: String,
	val city: String,
	val targetPrice: MoneyDto,
	val iconEmoji: String?,
	val note: String?,
	val createdAt: Instant,
)

data class CreateWishlistItemRequest(
	@field:NotBlank
	@field:Size(min = 1, max = 200)
	val name: String,
	@field:NotBlank
	@field:Size(min = 1, max = 120)
	val city: String,
	@field:NotNull
	@field:Valid
	val targetPrice: MoneyDto,
	@field:Size(max = 8)
	val iconEmoji: String? = null,
	@field:Size(max = 500)
	val note: String? = null,
)

data class UpdateWishlistItemRequest(
	@field:Size(min = 1, max = 200)
	val name: String? = null,
	@field:Size(min = 1, max = 120)
	val city: String? = null,
	@field:Valid
	val targetPrice: MoneyDto? = null,
	@field:Size(max = 8)
	val iconEmoji: String? = null,
	@field:Size(max = 500)
	val note: String? = null,
)

data class WishlistResponse(
	val items: List<WishlistItemDto>,
)
