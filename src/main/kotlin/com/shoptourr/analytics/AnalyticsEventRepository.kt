package com.shoptourr.analytics

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface AnalyticsEventRepository : JpaRepository<AnalyticsEvent, UUID> {

	fun existsByUserIdAndClientEventId(userId: UUID, clientEventId: String): Boolean
}
