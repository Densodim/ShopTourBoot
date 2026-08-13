package com.shoptourr.trip

import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface TripRepository : JpaRepository<Trip, UUID> {

	@EntityGraph(attributePaths = ["travelers"])
	fun findByIdAndDeletedAtIsNull(id: UUID): Trip?

	@EntityGraph(attributePaths = ["travelers"])
	fun findAllByOwnerIdAndDeletedAtIsNull(ownerId: UUID): List<Trip>

	fun countByOwnerIdAndDeletedAtIsNull(ownerId: UUID): Int
}
