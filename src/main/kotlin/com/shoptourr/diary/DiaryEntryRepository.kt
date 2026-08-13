package com.shoptourr.diary

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface DiaryEntryRepository : JpaRepository<DiaryEntry, UUID> {

	fun findByIdAndDeletedAtIsNull(id: UUID): DiaryEntry?

	fun findAllByTripIdAndDeletedAtIsNullOrderByEntryDateDescCreatedAtDesc(tripId: UUID): List<DiaryEntry>
}
