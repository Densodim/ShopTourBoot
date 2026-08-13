package com.shoptourr.idempotency

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface IdempotencyRecordRepository : JpaRepository<IdempotencyRecord, UUID> {

	fun findByUserIdAndRouteKeyAndIdempotencyKey(
		userId: UUID,
		routeKey: String,
		idempotencyKey: String,
	): IdempotencyRecord?
}
