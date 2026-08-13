package com.shoptourr.diary

import com.shoptourr.ResourceNotFoundException
import com.shoptourr.diary.dto.CreateDiaryEntryRequest
import com.shoptourr.diary.dto.DiaryDayGroupDto
import com.shoptourr.diary.dto.DiaryEntryDto
import com.shoptourr.diary.dto.TripDiaryResponse
import com.shoptourr.diary.dto.UpdateDiaryEntryRequest
import com.shoptourr.purchase.PurchaseService
import com.shoptourr.trip.TripService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Service
class DiaryService(
	private val entries: DiaryEntryRepository,
	private val tripService: TripService,
	private val clock: Clock,
) {

	@Transactional
	fun create(ownerId: UUID, tripId: UUID, request: CreateDiaryEntryRequest): DiaryEntryDto {
		tripService.requireOwned(ownerId, tripId)
		val now = Instant.now(clock)
		val entry = entries.save(
			DiaryEntry(
				tripId = tripId,
				entryDate = request.entryDate ?: LocalDate.now(clock),
				mood = request.mood.trim(),
				text = request.text.trim(),
				createdAt = now,
				updatedAt = now,
			),
		)
		return entry.toDto()
	}

	@Transactional(readOnly = true)
	fun list(ownerId: UUID, tripId: UUID): TripDiaryResponse {
		tripService.requireOwned(ownerId, tripId)
		val today = LocalDate.now(clock)
		val days = entries.findAllByTripIdAndDeletedAtIsNullOrderByEntryDateDescCreatedAtDesc(tripId)
			.groupBy { it.entryDate }
			.toSortedMap(compareByDescending { it })
			.map { (date, dayEntries) ->
				DiaryDayGroupDto(
					date = date,
					labelKey = PurchaseService.labelKey(date, today),
					entries = dayEntries.map { it.toDto() },
				)
			}
		return TripDiaryResponse(days)
	}

	@Transactional
	fun update(ownerId: UUID, tripId: UUID, entryId: UUID, request: UpdateDiaryEntryRequest): DiaryEntryDto {
		tripService.requireOwned(ownerId, tripId)
		val entry = requireOnTrip(tripId, entryId)
		request.mood?.trim()?.takeIf { it.isNotBlank() }?.let { entry.mood = it }
		request.text?.trim()?.takeIf { it.isNotBlank() }?.let { entry.text = it }
		entry.updatedAt = Instant.now(clock)
		return entry.toDto()
	}

	@Transactional
	fun delete(ownerId: UUID, tripId: UUID, entryId: UUID) {
		tripService.requireOwned(ownerId, tripId)
		requireOnTrip(tripId, entryId).deletedAt = Instant.now(clock)
	}

	private fun requireOnTrip(tripId: UUID, entryId: UUID): DiaryEntry {
		val entry = entries.findByIdAndDeletedAtIsNull(entryId)
		if (entry == null || entry.tripId != tripId) {
			throw ResourceNotFoundException("Diary entry not found.")
		}
		return entry
	}
}

fun DiaryEntry.toDto(): DiaryEntryDto =
	DiaryEntryDto(
		id = id,
		tripId = tripId,
		entryDate = entryDate,
		mood = mood,
		text = text,
		createdAt = createdAt,
		updatedAt = updatedAt,
	)
