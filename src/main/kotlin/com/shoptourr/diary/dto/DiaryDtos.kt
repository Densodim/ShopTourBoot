package com.shoptourr.diary.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class DiaryEntryDto(
	val id: UUID,
	val tripId: UUID,
	val entryDate: LocalDate,
	val mood: String,
	val text: String,
	val createdAt: Instant,
	val updatedAt: Instant,
)

data class CreateDiaryEntryRequest(
	val entryDate: LocalDate? = null,
	@field:NotBlank
	@field:Size(min = 1, max = 8)
	val mood: String,
	@field:NotBlank
	@field:Size(min = 1, max = 4000)
	val text: String,
)

data class UpdateDiaryEntryRequest(
	@field:Size(min = 1, max = 8)
	val mood: String? = null,
	@field:Size(min = 1, max = 4000)
	val text: String? = null,
)

data class DiaryDayGroupDto(
	val date: LocalDate,
	val labelKey: String,
	val entries: List<DiaryEntryDto>,
)

data class TripDiaryResponse(
	val days: List<DiaryDayGroupDto>,
)
