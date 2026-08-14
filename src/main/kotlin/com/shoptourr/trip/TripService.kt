package com.shoptourr.trip

import com.shoptourr.DomainValidationException
import com.shoptourr.ResourceConflictException
import com.shoptourr.ResourceNotFoundException
import com.shoptourr.fx.FxRateService
import com.shoptourr.identity.AppUser
import com.shoptourr.identity.AppUserRepository
import com.shoptourr.purchase.PurchaseRepository
import com.shoptourr.shared.dto.ExchangeRateDto
import com.shoptourr.shared.dto.MoneyDto
import com.shoptourr.trip.dto.CreateTravelerRequest
import com.shoptourr.trip.dto.CreateTripRequest
import com.shoptourr.trip.dto.InviteTravelerRequest
import com.shoptourr.trip.dto.TravelerDto
import com.shoptourr.trip.dto.TripDto
import com.shoptourr.trip.dto.TripInviteDto
import com.shoptourr.trip.dto.TripInviteStatus
import com.shoptourr.trip.dto.TripListResponse
import com.shoptourr.trip.dto.TripStatus
import com.shoptourr.trip.dto.TripSummaryDto
import com.shoptourr.trip.dto.UpdateTripRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import java.util.UUID

@Service
class TripService(
	private val trips: TripRepository,
	private val users: AppUserRepository,
	private val clock: Clock,
	private val purchases: PurchaseRepository,
	private val invites: TripInviteRepository,
	private val fxRates: FxRateService,
) {

	@Transactional
	fun create(ownerId: UUID, request: CreateTripRequest): TripDto {
		validateDates(request.startDate, request.endDate)
		val owner = requireUser(ownerId)
		val now = Instant.now(clock)
		val today = LocalDate.now(clock)
		val countryCode = request.countryCode?.uppercase()
		val trip = Trip(
			ownerId = ownerId,
			city = request.city.trim(),
			country = request.country.trim(),
			countryCode = countryCode,
			flagEmoji = flagEmoji(countryCode),
			status = TripStatus.UPCOMING.name,
			startDate = request.startDate,
			endDate = request.endDate,
			budgetAmount = request.budget.amount,
			budgetCurrency = request.budget.currency,
			defaultVatRatePercent = request.defaultVatRatePercent ?: BigDecimal.ZERO,
			createdAt = now,
			updatedAt = now,
		)
		applyFx(trip, request.quoteCurrency ?: owner.preferredCurrency, today)
		trip.status = resolveStatus(trip, today).name
		addTraveler(
			trip,
			name = owner.displayName,
			colorHex = OWNER_COLOR,
			avatarGlyph = glyph(owner.displayName),
			isOwner = true,
			userId = owner.id,
			now = now,
		)
		request.travelers.orEmpty().forEach { extra ->
			addTraveler(
				trip,
				name = extra.name.trim(),
				colorHex = extra.colorHex.uppercase(),
				avatarGlyph = extra.avatarGlyph?.trim()?.takeIf { it.isNotBlank() } ?: glyph(extra.name),
				isOwner = false,
				userId = null,
				now = now,
			)
		}
		return toDto(trips.save(trip), today)
	}

	@Transactional(readOnly = true)
	fun get(ownerId: UUID, tripId: UUID): TripDto =
		toDto(requireOwned(ownerId, tripId), LocalDate.now(clock))

	@Transactional(readOnly = true)
	fun list(ownerId: UUID): TripListResponse {
		val today = LocalDate.now(clock)
		val summaries = trips.findAllByOwnerIdAndDeletedAtIsNull(ownerId)
			.map { toSummary(it, today) }
		return TripListResponse(
			active = summaries.filter { it.status == TripStatus.ACTIVE },
			upcoming = summaries.filter { it.status == TripStatus.UPCOMING },
			past = summaries.filter { it.status == TripStatus.PAST || it.status == TripStatus.ARCHIVED },
		)
	}

	@Transactional
	fun update(ownerId: UUID, tripId: UUID, request: UpdateTripRequest): TripDto {
		val trip = requireOwned(ownerId, tripId)
		val start = request.startDate ?: trip.startDate
		val end = request.endDate ?: trip.endDate
		validateDates(start, end)
		request.city?.trim()?.takeIf { it.isNotBlank() }?.let { trip.city = it }
		request.country?.trim()?.takeIf { it.isNotBlank() }?.let { trip.country = it }
		request.countryCode?.uppercase()?.let {
			trip.countryCode = it
			trip.flagEmoji = flagEmoji(it)
		}
		trip.startDate = start
		trip.endDate = end
		request.budget?.let {
			trip.budgetAmount = it.amount
			trip.budgetCurrency = it.currency
		}
		request.defaultVatRatePercent?.let { trip.defaultVatRatePercent = it }
		if (request.status == TripStatus.ARCHIVED) {
			trip.status = TripStatus.ARCHIVED.name
		} else if (trip.status != TripStatus.ARCHIVED.name) {
			trip.status = resolveStatus(trip, LocalDate.now(clock)).name
		}
		trip.updatedAt = Instant.now(clock)
		return toDto(trip, LocalDate.now(clock))
	}

	@Transactional
	fun delete(ownerId: UUID, tripId: UUID) {
		val trip = requireOwned(ownerId, tripId)
		trip.deletedAt = Instant.now(clock)
	}

	@Transactional(readOnly = true)
	fun countsFor(ownerId: UUID): Pair<Int, Int> {
		val live = trips.findAllByOwnerIdAndDeletedAtIsNull(ownerId)
		return live.size to live.map { it.country }.distinct().size
	}

	fun requireOwned(ownerId: UUID, tripId: UUID): Trip {
		val trip = trips.findByIdAndDeletedAtIsNull(tripId)
		if (trip == null || trip.ownerId != ownerId) {
			throw ResourceNotFoundException("Trip not found.")
		}
		return trip
	}

	private fun requireUser(userId: UUID): AppUser {
		val user = users.findById(userId).orElse(null)
		if (user == null || user.deletedAt != null) {
			throw ResourceNotFoundException("User not found.")
		}
		return user
	}

	@Transactional
	fun addTraveler(ownerId: UUID, tripId: UUID, request: CreateTravelerRequest): TravelerDto {
		val trip = requireOwned(ownerId, tripId)
		addTraveler(
			trip,
			name = request.name.trim(),
			colorHex = request.colorHex.uppercase(),
			avatarGlyph = request.avatarGlyph?.trim()?.takeIf { it.isNotBlank() } ?: glyph(request.name),
			isOwner = false,
			userId = null,
			now = Instant.now(clock),
		)
		trip.updatedAt = Instant.now(clock)
		return trip.travelers.last().toDto()
	}

	@Transactional
	fun inviteTraveler(ownerId: UUID, tripId: UUID, request: InviteTravelerRequest): TripInviteDto {
		requireOwned(ownerId, tripId)
		val email = request.email.trim().lowercase()
		if (invites.findByTripIdAndEmailAndStatus(tripId, email, TripInviteStatus.PENDING.name) != null) {
			throw ResourceConflictException("An invite for this email is already pending.")
		}
		val now = Instant.now(clock)
		val invite = invites.save(
			TripInvite(
				tripId = tripId,
				email = email,
				displayNameHint = request.displayNameHint?.trim()?.takeIf { it.isNotBlank() },
				status = TripInviteStatus.PENDING.name,
				createdAt = now,
				expiresAt = now.plus(INVITE_TTL),
			),
		)
		return TripInviteDto(
			id = invite.id,
			tripId = invite.tripId,
			email = invite.email,
			status = TripInviteStatus.PENDING,
			createdAt = invite.createdAt,
			expiresAt = invite.expiresAt,
		)
	}

	@Transactional
	fun refreshExchangeRate(ownerId: UUID, tripId: UUID): ExchangeRateDto {
		val trip = requireOwned(ownerId, tripId)
		val today = LocalDate.now(clock)
		applyFx(trip, trip.fxQuoteCurrency ?: trip.budgetCurrency, today)
		trip.updatedAt = Instant.now(clock)
		return ExchangeRateDto(
			tripCurrency = trip.fxTripCurrency ?: trip.budgetCurrency,
			quoteCurrency = trip.fxQuoteCurrency ?: trip.budgetCurrency,
			rate = trip.fxRate ?: BigDecimal.ONE,
			rateDate = today.toString(),
			provider = trip.fxProvider,
		)
	}

	private fun addTraveler(
		trip: Trip,
		name: String,
		colorHex: String,
		avatarGlyph: String?,
		isOwner: Boolean,
		userId: UUID?,
		now: Instant,
	) {
		val traveler = Traveler(
			trip = trip,
			userId = userId,
			name = name,
			colorHex = colorHex,
			avatarGlyph = avatarGlyph,
			isOwner = isOwner,
			createdAt = now,
		)
		trip.travelers.add(traveler)
	}

	private fun applyFx(trip: Trip, quoteCurrency: String, today: LocalDate) {
		val quoteCode = quoteCurrency.uppercase()
		val quote = fxRates.quote(trip.budgetCurrency, quoteCode)
		trip.fxTripCurrency = trip.budgetCurrency
		trip.fxQuoteCurrency = quoteCode
		trip.fxRate = quote.rate
		trip.fxRateDate = today
		trip.fxProvider = quote.provider
	}

	private fun toDto(trip: Trip, today: LocalDate): TripDto {
		val status = resolveStatus(trip, today)
		val spent = MoneyDto(purchases.sumGrossByTripId(trip.id).setScale(2, RoundingMode.HALF_UP), trip.budgetCurrency)
		val budget = MoneyDto(trip.budgetAmount.setScale(2, RoundingMode.HALF_UP), trip.budgetCurrency)
		return TripDto(
			id = trip.id,
			city = trip.city,
			country = trip.country,
			countryCode = trip.countryCode,
			flagEmoji = trip.flagEmoji,
			status = status,
			startDate = trip.startDate,
			endDate = trip.endDate,
			datesLabel = datesLabel(trip.startDate, trip.endDate),
			budget = budget,
			spent = spent,
			remaining = MoneyDto(budget.amount.subtract(spent.amount), budget.currency),
			purchaseCount = purchases.countByTripIdAndDeletedAtIsNull(trip.id),
			dayCount = dayCount(trip.startDate, trip.endDate),
			currentDayNumber = currentDay(status, trip.startDate, trip.endDate, today),
			defaultVatRatePercent = trip.defaultVatRatePercent,
			exchangeRate = trip.fxRate?.let { rate ->
				ExchangeRateDto(
					tripCurrency = trip.fxTripCurrency ?: trip.budgetCurrency,
					quoteCurrency = trip.fxQuoteCurrency ?: trip.budgetCurrency,
					rate = rate,
					rateDate = (trip.fxRateDate ?: today).toString(),
					provider = trip.fxProvider,
				)
			},
			travelers = trip.travelers.filter { it.deletedAt == null }.map { it.toDto() },
			createdAt = trip.createdAt,
			updatedAt = trip.updatedAt,
		)
	}

	private fun toSummary(trip: Trip, today: LocalDate): TripSummaryDto {
		val dto = toDto(trip, today)
		return TripSummaryDto(
			id = dto.id,
			city = dto.city,
			country = dto.country,
			flagEmoji = dto.flagEmoji,
			status = dto.status,
			startDate = dto.startDate,
			endDate = dto.endDate,
			datesLabel = dto.datesLabel,
			budget = dto.budget,
			spent = dto.spent,
			purchaseCount = dto.purchaseCount,
			currentDayNumber = dto.currentDayNumber,
			dayCount = dto.dayCount,
		)
	}

	companion object {
		const val OWNER_COLOR = "#FFD84D"
		private val INVITE_TTL = Duration.ofDays(7)
		private val MONTH = DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH)
		private val FLAGS = mapOf(
			"PT" to "🇵🇹",
			"JP" to "🇯🇵",
			"NO" to "🇳🇴",
			"IT" to "🇮🇹",
			"FR" to "🇫🇷",
			"ES" to "🇪🇸",
			"DE" to "🇩🇪",
			"GB" to "🇬🇧",
			"US" to "🇺🇸",
		)

		fun validateDates(start: LocalDate, end: LocalDate) {
			if (end.isBefore(start)) {
				throw DomainValidationException("endDate must be on or after startDate")
			}
		}

		fun resolveStatus(trip: Trip, today: LocalDate): TripStatus {
			if (trip.status == TripStatus.ARCHIVED.name) return TripStatus.ARCHIVED
			return when {
				today.isBefore(trip.startDate) -> TripStatus.UPCOMING
				today.isAfter(trip.endDate) -> TripStatus.PAST
				else -> TripStatus.ACTIVE
			}
		}

		fun dayCount(start: LocalDate, end: LocalDate): Int =
			ChronoUnit.DAYS.between(start, end).toInt() + 1

		fun currentDay(status: TripStatus, start: LocalDate, end: LocalDate, today: LocalDate): Int? {
			if (status != TripStatus.ACTIVE) return null
			val day = ChronoUnit.DAYS.between(start, today).toInt() + 1
			return day.coerceIn(1, dayCount(start, end))
		}

		fun datesLabel(start: LocalDate, end: LocalDate): String {
			val startMonth = start.format(MONTH).uppercase()
			val endMonth = end.format(MONTH).uppercase()
			return if (start.month == end.month && start.year == end.year) {
				"${start.dayOfMonth}–${end.dayOfMonth} $startMonth"
			} else {
				"${start.dayOfMonth} $startMonth – ${end.dayOfMonth} $endMonth"
			}
		}

		fun flagEmoji(countryCode: String?): String? = countryCode?.let { FLAGS[it] }

		fun glyph(name: String): String =
			name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"
	}
}

fun Traveler.toDto(): TravelerDto =
	TravelerDto(
		id = id,
		name = name,
		colorHex = colorHex,
		avatarGlyph = avatarGlyph,
		isOwner = isOwner,
	)
