package com.shoptourr.wishlist

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface WishlistItemRepository : JpaRepository<WishlistItem, UUID> {

	fun findByIdAndDeletedAtIsNull(id: UUID): WishlistItem?

	fun findAllByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(userId: UUID): List<WishlistItem>

	fun countByUserIdAndDeletedAtIsNull(userId: UUID): Int
}
