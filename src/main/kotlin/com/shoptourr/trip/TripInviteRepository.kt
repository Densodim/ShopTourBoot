package com.shoptourr.trip

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface TripInviteRepository : JpaRepository<TripInvite, UUID> {

	fun findByTripIdAndEmailAndStatus(tripId: UUID, email: String, status: String): TripInvite?
}
