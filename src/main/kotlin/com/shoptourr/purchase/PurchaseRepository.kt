package com.shoptourr.purchase

import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

interface PurchaseRepository : JpaRepository<Purchase, UUID> {

	@EntityGraph(attributePaths = ["splitTravelerIds"])
	fun findByIdAndDeletedAtIsNull(id: UUID): Purchase?

	@EntityGraph(attributePaths = ["splitTravelerIds"])
	fun findAllByTripIdAndDeletedAtIsNullOrderByPurchaseDateDescPurchaseTimeDesc(tripId: UUID): List<Purchase>

	fun countByTripIdAndDeletedAtIsNull(tripId: UUID): Int

	@EntityGraph(attributePaths = ["splitTravelerIds"])
	fun findByTripIdAndDeletedAtIsNullOrderByPurchaseDateDescIdDesc(
		tripId: UUID,
		pageable: Pageable,
	): List<Purchase>

	@EntityGraph(attributePaths = ["splitTravelerIds"])
	@Query(
		"""
		select p from Purchase p
		where p.tripId = :tripId
		  and p.deletedAt is null
		  and (
		    p.purchaseDate < :afterDate
		    or (p.purchaseDate = :afterDate and p.id < :afterId)
		  )
		order by p.purchaseDate desc, p.id desc
		""",
	)
	fun findPageAfterCursor(
		tripId: UUID,
		afterDate: LocalDate,
		afterId: UUID,
		pageable: Pageable,
	): List<Purchase>

	@Query("select coalesce(sum(p.grossAmount), 0) from Purchase p where p.tripId = :tripId and p.deletedAt is null")
	fun sumGrossByTripId(tripId: UUID): BigDecimal
}
