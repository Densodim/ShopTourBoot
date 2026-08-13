package com.shoptourr.wishlist

import com.shoptourr.ResourceNotFoundException
import com.shoptourr.shared.dto.MoneyDto
import com.shoptourr.wishlist.dto.CreateWishlistItemRequest
import com.shoptourr.wishlist.dto.UpdateWishlistItemRequest
import com.shoptourr.wishlist.dto.WishlistItemDto
import com.shoptourr.wishlist.dto.WishlistResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Clock
import java.time.Instant
import java.util.UUID

@Service
class WishlistService(
	private val items: WishlistItemRepository,
	private val clock: Clock,
) {

	@Transactional
	fun create(userId: UUID, request: CreateWishlistItemRequest): WishlistItemDto {
		val now = Instant.now(clock)
		return items.save(
			WishlistItem(
				userId = userId,
				name = request.name.trim(),
				city = request.city.trim(),
				targetAmount = request.targetPrice.amount,
				targetCurrency = request.targetPrice.currency,
				iconEmoji = request.iconEmoji,
				note = request.note?.trim()?.takeIf { it.isNotBlank() },
				createdAt = now,
				updatedAt = now,
			),
		).toDto()
	}

	@Transactional(readOnly = true)
	fun list(userId: UUID): WishlistResponse =
		WishlistResponse(items.findAllByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(userId).map { it.toDto() })

	@Transactional
	fun update(userId: UUID, itemId: UUID, request: UpdateWishlistItemRequest): WishlistItemDto {
		val item = requireOwned(userId, itemId)
		request.name?.trim()?.takeIf { it.isNotBlank() }?.let { item.name = it }
		request.city?.trim()?.takeIf { it.isNotBlank() }?.let { item.city = it }
		request.targetPrice?.let {
			item.targetAmount = it.amount
			item.targetCurrency = it.currency
		}
		request.iconEmoji?.let { item.iconEmoji = it }
		request.note?.let { item.note = it.trim().takeIf { value -> value.isNotBlank() } }
		item.updatedAt = Instant.now(clock)
		return item.toDto()
	}

	@Transactional
	fun delete(userId: UUID, itemId: UUID) {
		requireOwned(userId, itemId).deletedAt = Instant.now(clock)
	}

	fun countFor(userId: UUID): Int = items.countByUserIdAndDeletedAtIsNull(userId)

	private fun requireOwned(userId: UUID, itemId: UUID): WishlistItem {
		val item = items.findByIdAndDeletedAtIsNull(itemId)
		if (item == null || item.userId != userId) {
			throw ResourceNotFoundException("Wishlist item not found.")
		}
		return item
	}
}

fun WishlistItem.toDto(): WishlistItemDto =
	WishlistItemDto(
		id = id,
		name = name,
		city = city,
		targetPrice = MoneyDto(targetAmount.setScale(2, RoundingMode.HALF_UP), targetCurrency),
		iconEmoji = iconEmoji,
		note = note,
		createdAt = createdAt,
	)
