package com.shoptourr.push

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface PushDeviceRepository : JpaRepository<PushDevice, UUID> {

	fun findByUserIdAndTokenHashAndDeletedAtIsNull(userId: UUID, tokenHash: String): PushDevice?

	fun findByIdAndDeletedAtIsNull(id: UUID): PushDevice?
}
