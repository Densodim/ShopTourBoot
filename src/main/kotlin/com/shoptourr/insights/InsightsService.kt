package com.shoptourr.insights

import com.shoptourr.identity.MeService
import com.shoptourr.insights.dto.AlertSeverity
import com.shoptourr.insights.dto.AlertType
import com.shoptourr.insights.dto.BudgetAlertDto
import com.shoptourr.insights.dto.CategorySpendDto
import com.shoptourr.insights.dto.DailySpendDto
import com.shoptourr.insights.dto.GeoPointDto
import com.shoptourr.insights.dto.HomeResponse
import com.shoptourr.insights.dto.RouteStopDto
import com.shoptourr.insights.dto.TaxFreeEligibleItemDto
import com.shoptourr.insights.dto.TaxFreeRulesDto
import com.shoptourr.insights.dto.TaxFreeSummaryDto
import com.shoptourr.insights.dto.TripAlertsResponse
import com.shoptourr.insights.dto.TripRouteDto
import com.shoptourr.insights.dto.TripStatsDto
import com.shoptourr.purchase.Purchase
import com.shoptourr.purchase.PurchaseRepository
import com.shoptourr.purchase.dto.PurchaseCategory
import com.shoptourr.shared.dto.MoneyDto
import com.shoptourr.trip.Trip
import com.shoptourr.trip.TripService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.UUID

@Service
class InsightsService(
	private val meService: MeService,
	private val tripService: TripService,
	private val purchases: PurchaseRepository,
	private val clock: Clock,
) {

	@Transactional(readOnly = true)
	fun home(userId: UUID): HomeResponse {
		val user = meService.getMe(userId)
		val list = tripService.list(userId)
		val current = list.active.firstOrNull()
		val spent = (list.active + list.past).fold(BigDecimal.ZERO) { acc, trip -> acc.add(trip.spent.amount) }
			.setScale(2, RoundingMode.HALF_UP)
		val alerts = current?.let { alerts(userId, it.id).alerts.size } ?: 0
		return HomeResponse(
			user = user,
			currentTrip = current,
			upcoming = list.upcoming,
			archive = list.past,
			allTimeSpent = MoneyDto(spent, user.preferredCurrency),
			unreadAlertCount = alerts,
		)
	}

	@Transactional(readOnly = true)
	fun stats(userId: UUID, tripId: UUID): TripStatsDto {
		val trip = tripService.requireOwned(userId, tripId)
		val items = livePurchases(tripId)
		val currency = trip.budgetCurrency
		val total = sum(items)
		val budget = trip.budgetAmount.setScale(2, RoundingMode.HALF_UP)
		val days = TripService.dayCount(trip.startDate, trip.endDate).coerceAtLeast(1)
		val elapsed = ChronoUnit.DAYS.between(trip.startDate, LocalDate.now(clock)).toInt().coerceIn(1, days)
		val dailyAverage = total.divide(BigDecimal(elapsed), 2, RoundingMode.HALF_UP)
		val byCategory = items.groupBy { it.category }.map { (category, group) ->
			val amount = sum(group)
			CategorySpendDto(
				category = PurchaseCategory.valueOf(category),
				amount = MoneyDto(amount, currency),
				share = if (total.signum() == 0) BigDecimal.ZERO else amount.divide(total, 4, RoundingMode.HALF_UP),
				purchaseCount = group.size,
			)
		}.sortedByDescending { it.amount.amount }
		val byDay = items.groupBy { it.purchaseDate }.toSortedMap().map { (date, group) ->
			DailySpendDto(date, MoneyDto(sum(group), currency), group.size)
		}
		val remaining = budget.subtract(total)
		val expectedByNow = if (budget.signum() == 0) BigDecimal.ZERO
		else budget.divide(BigDecimal(days), 4, RoundingMode.HALF_UP).multiply(BigDecimal(elapsed))
		val dailyBudget = if (days == 0) BigDecimal.ZERO else budget.divide(BigDecimal(days), 4, RoundingMode.HALF_UP)
		val paceDelta = if (dailyBudget.signum() == 0) null
		else remaining.divide(dailyBudget, 0, RoundingMode.HALF_UP).toInt()
		return TripStatsDto(
			tripId = trip.id,
			totalSpent = MoneyDto(total, currency),
			budget = MoneyDto(budget, currency),
			dailyAverage = MoneyDto(dailyAverage, currency),
			remaining = MoneyDto(remaining, currency),
			onBudget = total <= budget,
			paceDeltaDays = paceDelta,
			topCategory = byCategory.firstOrNull()?.category,
			byCategory = byCategory,
			byDay = byDay,
		)
	}

	@Transactional(readOnly = true)
	fun alerts(userId: UUID, tripId: UUID): TripAlertsResponse {
		val stats = stats(userId, tripId)
		val now = Instant.now(clock)
		val alerts = buildList {
			if (stats.totalSpent.amount > stats.budget.amount) {
				add(alert(AlertType.BUDGET_EXCEEDED, AlertSeverity.CRITICAL, tripId, now, stats.remaining, null))
			} else if (stats.totalSpent.amount >= stats.budget.amount.multiply(BigDecimal("0.80"))) {
				add(alert(AlertType.BUDGET_ALMOST_GONE, AlertSeverity.WARNING, tripId, now, stats.remaining, null))
			}
			if ((stats.paceDeltaDays ?: 0) < 0) {
				add(alert(AlertType.PACE_HIGH, AlertSeverity.WARNING, tripId, now, stats.remaining, stats.topCategory))
			}
		}
		return TripAlertsResponse(alerts)
	}

	@Transactional(readOnly = true)
	fun taxFree(userId: UUID, tripId: UUID): TaxFreeSummaryDto {
		val trip = tripService.requireOwned(userId, tripId)
		val currency = trip.budgetCurrency
		val rules = rulesFor(trip)
		val items = livePurchases(tripId).filter { it.taxRefundEligible }.map { purchase ->
			val amount = purchase.grossAmount.setScale(2, RoundingMode.HALF_UP)
			val refund = amount.multiply(rules.estimatedRefundRate).setScale(2, RoundingMode.HALF_UP)
			TaxFreeEligibleItemDto(
				purchaseId = purchase.id,
				name = purchase.name,
				amount = MoneyDto(amount, currency),
				estimatedRefund = MoneyDto(refund, currency),
				meetsMinimum = amount >= rules.minimumPurchase.amount,
			)
		}
		val eligible = items.filter { it.meetsMinimum }
		val eligibleTotal = eligible.fold(BigDecimal.ZERO) { acc, item -> acc.add(item.amount.amount) }
		val refundTotal = eligible.fold(BigDecimal.ZERO) { acc, item -> acc.add(item.estimatedRefund.amount) }
		val remaining = if (eligible.isNotEmpty()) null
		else rules.minimumPurchase.amount.subtract(items.maxOfOrNull { it.amount.amount } ?: BigDecimal.ZERO)
			.max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP)
		return TaxFreeSummaryDto(
			tripId = trip.id,
			rules = rules,
			eligibleCount = eligible.size,
			eligibleTotal = MoneyDto(eligibleTotal.setScale(2, RoundingMode.HALF_UP), currency),
			estimatedRefundTotal = MoneyDto(refundTotal.setScale(2, RoundingMode.HALF_UP), currency),
			remainingToMinimum = remaining?.let { MoneyDto(it, currency) },
			items = items,
		)
	}

	@Transactional(readOnly = true)
	fun route(userId: UUID, tripId: UUID): TripRouteDto {
		val trip = tripService.requireOwned(userId, tripId)
		val stops = livePurchases(tripId)
			.filter { !it.place.isNullOrBlank() }
			.groupBy { it.place!!.trim() }
			.entries
			.sortedBy { entry -> entry.value.minOf { it.purchaseDate } }
			.mapIndexed { index, (place, group) ->
				val resolved = PlaceCatalog.resolve(place, trip.city, trip.country, trip.countryCode)
				RouteStopDto(
					id = UUID.nameUUIDFromBytes("$tripId:$place".toByteArray()),
					title = place,
					place = place,
					date = group.minOf { it.purchaseDate },
					amountSpentHere = MoneyDto(sum(group), trip.budgetCurrency),
					point = resolved?.let { GeoPointDto(it.lat, it.lng) },
					orderIndex = index,
				)
			}
		val coords = stops.mapNotNull { stop ->
			val lat = stop.point?.lat
			val lng = stop.point?.lng
			if (lat != null && lng != null) PlaceCatalog.LatLng(lat, lng) else null
		}
		val path = coords.map { GeoPointDto(it.lat, it.lng) }
		val distance = coords.zipWithNext()
			.fold(BigDecimal.ZERO) { acc, (from, to) -> acc.add(GeoMath.meters(from, to)) }
			.takeIf { coords.size >= 2 }
		return TripRouteDto(trip.id, stops.size, distance, stops, path)
	}

	private fun livePurchases(tripId: UUID): List<Purchase> =
		purchases.findAllByTripIdAndDeletedAtIsNullOrderByPurchaseDateDescPurchaseTimeDesc(tripId)

	private fun sum(items: List<Purchase>): BigDecimal =
		items.fold(BigDecimal.ZERO) { acc, item -> acc.add(item.grossAmount) }.setScale(2, RoundingMode.HALF_UP)

	private fun rulesFor(trip: Trip): TaxFreeRulesDto {
		val currency = trip.budgetCurrency
		val minimum = when (trip.countryCode) {
			"PT" -> BigDecimal("50.00")
			"JP" -> BigDecimal("5000.00")
			else -> BigDecimal("100.00")
		}
		val rate = when (trip.countryCode) {
			"PT" -> BigDecimal("0.13")
			"JP" -> BigDecimal("0.10")
			else -> BigDecimal("0.10")
		}
		return TaxFreeRulesDto(
			currency = currency,
			minimumPurchase = MoneyDto(minimum, currency),
			estimatedRefundRate = rate,
			regionLabel = trip.country,
		)
	}

	private fun alert(
		type: AlertType,
		severity: AlertSeverity,
		tripId: UUID,
		now: Instant,
		remaining: MoneyDto?,
		category: PurchaseCategory?,
	) = BudgetAlertDto(
		id = UUID.nameUUIDFromBytes("$tripId:$type".toByteArray()),
		type = type,
		severity = severity,
		titleKey = "alert.${type.name.lowercase()}.title",
		bodyKey = "alert.${type.name.lowercase()}.body",
		params = emptyMap(),
		dailyRemaining = remaining,
		category = category,
		createdAt = now,
		read = false,
	)
}
