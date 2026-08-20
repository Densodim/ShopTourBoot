package com.shoptourr.wishlist.dto

import com.shoptourr.shared.dto.MoneyDto
import com.shoptourr.shared.validation.FieldPatterns
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
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
	@field:Pattern(regexp = FieldPatterns.ITEM_OR_PLACE)
	val name: String,
	@field:NotBlank
	@field:Size(min = 1, max = 120)
	@field:Pattern(regexp = FieldPatterns.PERSON_OR_PLACE)
	val city: String,
	@field:NotNull
	@field:Valid
	val targetPrice: MoneyDto,
	@field:Size(max = 8)
	@field:Pattern(regexp = FieldPatterns.MOOD)
	val iconEmoji: String? = null,
	@field:Size(max = 500)
	@field:Pattern(regexp = FieldPatterns.OPTIONAL_TEXT)
	val note: String? = null,
)

data class UpdateWishlistItemRequest(
	@field:Size(min = 1, max = 200)
	@field:Pattern(regexp = FieldPatterns.ITEM_OR_PLACE)
	val name: String? = null,
	@field:Size(min = 1, max = 120)
	@field:Pattern(regexp = FieldPatterns.PERSON_OR_PLACE)
	val city: String? = null,
	@field:Valid
	val targetPrice: MoneyDto? = null,
	@field:Size(max = 8)
	@field:Pattern(regexp = FieldPatterns.MOOD)
	val iconEmoji: String? = null,
	@field:Size(max = 500)
	@field:Pattern(regexp = FieldPatterns.OPTIONAL_TEXT)
	val note: String? = null,
)

data class WishlistResponse(
	val items: List<WishlistItemDto>,
)
