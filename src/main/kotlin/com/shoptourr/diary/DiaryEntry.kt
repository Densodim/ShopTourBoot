package com.shoptourr.diary

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Entity
@Table(name = "diary_entry")
class DiaryEntry(
	@Id
	val id: UUID = UUID.randomUUID(),

	@Column(name = "trip_id", nullable = false)
	val tripId: UUID,

	@Column(name = "entry_date", nullable = false)
	var entryDate: LocalDate,

	@Column(nullable = false, length = 8)
	var mood: String,

	@Column(nullable = false, length = 4000)
	var text: String,

	@Column(name = "created_at", nullable = false)
	val createdAt: Instant,

	@Column(name = "updated_at", nullable = false)
	var updatedAt: Instant,

	@Column(name = "deleted_at")
	var deletedAt: Instant? = null,
)
