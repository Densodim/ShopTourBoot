package com.shoptourr.purchase

import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.math.BigDecimal
import java.util.UUID

interface PurchaseRepository : JpaRepository<Purchase, UUID> {

	@EntityGraph(attributePaths = ["splitTravelerIds"])
	fun findByIdAndDeletedAtIsNull(id: UUID): Purchase?

	@EntityGraph(attributePaths = ["splitTravelerIds"])
	fun findAllByTripIdAndDeletedAtIsNullOrderByPurchaseDateDescPurchaseTimeDesc(tripId: UUID): List<Purchase>

	fun countByTripIdAndDeletedAtIsNull(tripId: UUID): Int

	@Query("select coalesce(sum(p.grossAmount), 0) from Purchase p where p.tripId = :tripId and p.deletedAt is null")
	fun sumGrossByTripId(tripId: UUID): BigDecimal
}
