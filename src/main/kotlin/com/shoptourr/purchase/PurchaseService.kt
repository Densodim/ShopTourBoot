package com.shoptourr.purchase

import com.shoptourr.DomainValidationException
import com.shoptourr.ResourceConflictException
import com.shoptourr.ResourceNotFoundException
import com.shoptourr.purchase.dto.CreatePurchaseRequest
import com.shoptourr.purchase.dto.PurchaseCategory
import com.shoptourr.purchase.dto.PurchaseDayGroupDto
import com.shoptourr.purchase.dto.PurchaseDto
import com.shoptourr.purchase.dto.SplitShareDto
import com.shoptourr.purchase.dto.TripPurchasesResponse
import com.shoptourr.purchase.dto.UpdatePurchaseRequest
import com.shoptourr.shared.dto.MoneyDto
import com.shoptourr.shared.dto.VatBreakdownDto
import com.shoptourr.media.MediaService
import com.shoptourr.push.PushService
import com.shoptourr.trip.Trip
import com.shoptourr.trip.TripService
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

@Service
class PurchaseService(
	private val purchases: PurchaseRepository,
	private val tripService: TripService,
	private val mediaService: MediaService,
	private val pushService: PushService,
	private val clock: Clock,
) {

	@Transactional
	fun create(ownerId: UUID, tripId: UUID, request: CreatePurchaseRequest): PurchaseDto {
		val trip = tripService.requireOwned(ownerId, tripId)
		val now = Instant.now(clock)
		val vat = VatCalculator.breakdown(
			request.amount.amount,
			request.vatRatePercent ?: trip.defaultVatRatePercent,
			request.vatIncluded,
		)
		val purchase = Purchase(
			tripId = trip.id,
			name = request.name.trim(),
			category = request.category.name,
			grossAmount = vat.gross,
			currency = request.amount.currency,
			netAmount = vat.net,
			vatAmount = vat.vat,
			vatRatePercent = vat.vatRatePercent,
			vatIncluded = vat.vatIncluded,
			taxRefundEligible = request.taxRefundEligible,
			place = request.place?.trim()?.takeIf { it.isNotBlank() },
			purchaseDate = request.purchaseDate ?: LocalDate.now(clock),
			purchaseTime = request.purchaseTime ?: LocalTime.now(clock).withNano(0),
			receiptMediaId = request.receiptMediaId,
			createdAt = now,
			updatedAt = now,
		)
		val before = spent(tripId)
		purchase.splitTravelerIds.addAll(resolveSplits(trip, request.splitWithTravelerIds))
		val saved = purchases.save(purchase)
		notifyBudget(trip.ownerId, trip.id, before, before.add(saved.grossAmount), trip.budgetAmount)
		return toDto(saved, trip)
	}

	@Transactional(readOnly = true)
	fun get(ownerId: UUID, tripId: UUID, purchaseId: UUID): PurchaseDto {
		val trip = tripService.requireOwned(ownerId, tripId)
		return toDto(requireOnTrip(tripId, purchaseId), trip)
	}

	@Transactional(readOnly = true)
	fun list(
		ownerId: UUID,
		tripId: UUID,
		afterDate: LocalDate? = null,
		afterId: UUID? = null,
		size: Int = DEFAULT_PAGE_SIZE,
	): TripPurchasesResponse {
		val trip = tripService.requireOwned(ownerId, tripId)
		if ((afterDate == null) != (afterId == null)) {
			throw DomainValidationException("afterDate and afterId must be used together")
		}
		if (size !in 1..MAX_PAGE_SIZE) {
			throw DomainValidationException("size must be between 1 and $MAX_PAGE_SIZE")
		}
		val pageable = PageRequest.of(0, size)
		val items = if (afterDate == null || afterId == null) {
			purchases.findByTripIdAndDeletedAtIsNullOrderByPurchaseDateDescIdDesc(tripId, pageable)
		} else {
			purchases.findPageAfterCursor(tripId, afterDate, afterId, pageable)
		}
		val today = LocalDate.now(clock)
		val currency = trip.budgetCurrency
		val spent = purchases.sumGrossByTripId(tripId).money()
		val days = items.groupBy { it.purchaseDate }.toSortedMap(compareByDescending { it }).map { (date, dayItems) ->
			val dtos = dayItems.map { toDto(it, trip) }
			PurchaseDayGroupDto(
				date = date,
				labelKey = labelKey(date, today),
				dayTotal = MoneyDto(dayItems.fold(BigDecimal.ZERO) { acc, item -> acc.add(item.grossAmount) }.money(), currency),
				items = dtos,
			)
		}
		val budget = MoneyDto(trip.budgetAmount, currency)
		return TripPurchasesResponse(
			spentTotal = MoneyDto(spent, currency),
			budget = budget,
			remaining = MoneyDto(budget.amount.subtract(spent), currency),
			days = days,
		)
	}

	@Transactional
	fun update(
		ownerId: UUID,
		tripId: UUID,
		purchaseId: UUID,
		request: UpdatePurchaseRequest,
		ifMatch: String? = null,
	): PurchaseDto {
		val trip = tripService.requireOwned(ownerId, tripId)
		val purchase = requireOnTrip(tripId, purchaseId)
		PurchaseEtag.parse(ifMatch)?.let { expected ->
			if (expected != purchase.updatedAt) {
				throw ResourceConflictException("Purchase was modified.")
			}
		}
		val before = spent(tripId)
		val oldGross = purchase.grossAmount
		request.name?.trim()?.takeIf { it.isNotBlank() }?.let { purchase.name = it }
		request.category?.let { purchase.category = it.name }
		request.taxRefundEligible?.let { purchase.taxRefundEligible = it }
		request.place?.let { purchase.place = it.trim().takeIf { value -> value.isNotBlank() } }
		request.purchaseDate?.let { purchase.purchaseDate = it }
		request.purchaseTime?.let { purchase.purchaseTime = it }
		request.receiptMediaId?.let { purchase.receiptMediaId = it }
		if (request.amount != null || request.vatIncluded != null || request.vatRatePercent != null) {
			val amount = request.amount?.amount ?: purchase.grossAmount
			val included = request.vatIncluded ?: purchase.vatIncluded
			val rate = request.vatRatePercent ?: purchase.vatRatePercent
			val vat = VatCalculator.breakdown(amount, rate, included)
			purchase.grossAmount = vat.gross
			purchase.netAmount = vat.net
			purchase.vatAmount = vat.vat
			purchase.vatRatePercent = vat.vatRatePercent
			purchase.vatIncluded = vat.vatIncluded
			request.amount?.currency?.let { purchase.currency = it }
		}
		request.splitWithTravelerIds?.let {
			purchase.splitTravelerIds.clear()
			purchase.splitTravelerIds.addAll(resolveSplits(trip, it))
		}
		purchase.updatedAt = Instant.now(clock)
		notifyBudget(trip.ownerId, trip.id, before, before.subtract(oldGross).add(purchase.grossAmount), trip.budgetAmount)
		return toDto(purchase, trip)
	}

	@Transactional
	fun delete(ownerId: UUID, tripId: UUID, purchaseId: UUID) {
		tripService.requireOwned(ownerId, tripId)
		val purchase = requireOnTrip(tripId, purchaseId)
		purchase.deletedAt = Instant.now(clock)
	}

	private fun requireOnTrip(tripId: UUID, purchaseId: UUID): Purchase {
		val purchase = purchases.findByIdAndDeletedAtIsNull(purchaseId)
		if (purchase == null || purchase.tripId != tripId) {
			throw ResourceNotFoundException("Purchase not found.")
		}
		return purchase
	}

	private fun resolveSplits(trip: Trip, requested: List<UUID>?): Set<UUID> {
		val live = trip.travelers.filter { it.deletedAt == null }
		if (requested.isNullOrEmpty()) {
			return setOf(live.first { it.isOwner }.id)
		}
		val allowed = live.map { it.id }.toSet()
		if (!allowed.containsAll(requested)) {
			throw DomainValidationException("splitWithTravelerIds must belong to this trip")
		}
		return requested.toSet()
	}

	private fun spent(tripId: UUID): BigDecimal =
		purchases.findAllByTripIdAndDeletedAtIsNullOrderByPurchaseDateDescPurchaseTimeDesc(tripId)
			.fold(BigDecimal.ZERO) { acc, item -> acc.add(item.grossAmount) }

	private fun notifyBudget(
		userId: UUID,
		tripId: UUID,
		before: BigDecimal,
		after: BigDecimal,
		budget: BigDecimal,
	) {
		if (PushService.crossing(before, after, budget) == null) {
			return
		}
		val send = {
			pushService.notifyBudgetCrossing(userId, tripId, before, after, budget)
		}
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.registerSynchronization(
				object : TransactionSynchronization {
					override fun afterCommit() = send()
				},
			)
		} else {
			send()
		}
	}

	private fun toDto(purchase: Purchase, trip: Trip): PurchaseDto {
		val amount = MoneyDto(purchase.grossAmount.money(), purchase.currency)
		val travelers = trip.travelers.filter { it.deletedAt == null }.associateBy { it.id }
		val splitIds = purchase.splitTravelerIds.toList()
		val shareAmount = if (splitIds.isEmpty()) purchase.grossAmount
		else purchase.grossAmount.divide(BigDecimal(splitIds.size), 2, RoundingMode.HALF_UP)
		val splits = splitIds.map { id ->
			val traveler = travelers[id]
			SplitShareDto(
				travelerId = id,
				travelerName = traveler?.name ?: "Traveler",
				share = MoneyDto(shareAmount, purchase.currency),
			)
		}
		val quote = trip.fxRate?.let { rate ->
			MoneyDto(
				purchase.grossAmount.multiply(rate).setScale(2, RoundingMode.HALF_UP),
				trip.fxQuoteCurrency ?: purchase.currency,
			)
		}
		return PurchaseDto(
			id = purchase.id,
			tripId = purchase.tripId,
			name = purchase.name,
			category = PurchaseCategory.valueOf(purchase.category),
			amount = amount,
			vat = VatBreakdownDto(
				net = purchase.netAmount.money(),
				vat = purchase.vatAmount.money(),
				gross = purchase.grossAmount.money(),
				vatRatePercent = purchase.vatRatePercent,
				vatIncluded = purchase.vatIncluded,
			),
			taxRefundEligible = purchase.taxRefundEligible,
			place = purchase.place,
			purchaseDate = purchase.purchaseDate,
			purchaseTime = purchase.purchaseTime,
			receiptMediaId = purchase.receiptMediaId,
			receiptThumbnailUrl = purchase.receiptMediaId?.let { mediaService.publicUrlIfReady(trip.ownerId, it) },
			splitWithTravelerIds = splitIds,
			splits = splits,
			yourShare = MoneyDto(shareAmount, purchase.currency),
			quoteEquivalent = quote,
			createdAt = purchase.createdAt,
			updatedAt = purchase.updatedAt,
		)
	}

	companion object {
		const val DEFAULT_PAGE_SIZE = 50
		const val MAX_PAGE_SIZE = 100

		fun labelKey(date: LocalDate, today: LocalDate): String = when (date) {
			today -> "today"
			today.minusDays(1) -> "yesterday"
			else -> date.toString()
		}
	}
}

private fun BigDecimal.money(): BigDecimal = setScale(2, RoundingMode.HALF_UP)
