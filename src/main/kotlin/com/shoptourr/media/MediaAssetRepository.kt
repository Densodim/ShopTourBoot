package com.shoptourr.media

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface MediaAssetRepository : JpaRepository<MediaAsset, UUID> {

	fun findByIdAndDeletedAtIsNull(id: UUID): MediaAsset?
}
