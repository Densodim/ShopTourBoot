package com.shoptourr.insights.dto

import com.shoptourr.identity.dto.UserDto
import com.shoptourr.purchase.dto.PurchaseCategory
import com.shoptourr.shared.dto.MoneyDto
import com.shoptourr.trip.dto.TripSummaryDto
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class HomeResponse(
	val user: UserDto,
	val currentTrip: TripSummaryDto?,
	val upcoming: List<TripSummaryDto>,
	val archive: List<TripSummaryDto>,
	val allTimeSpent: MoneyDto,
	val unreadAlertCount: Int,
)

data class CategorySpendDto(
	val category: PurchaseCategory,
	val amount: MoneyDto,
	val share: BigDecimal,
	val purchaseCount: Int,
)

data class DailySpendDto(
	val date: LocalDate,
	val amount: MoneyDto,
	val purchaseCount: Int,
)

data class TripStatsDto(
	val tripId: UUID,
	val totalSpent: MoneyDto,
	val budget: MoneyDto,
	val dailyAverage: MoneyDto,
	val remaining: MoneyDto,
	val onBudget: Boolean,
	val paceDeltaDays: Int?,
	val topCategory: PurchaseCategory?,
	val byCategory: List<CategorySpendDto>,
	val byDay: List<DailySpendDto>,
)

enum class AlertSeverity { INFO, WARNING, CRITICAL }

enum class AlertType {
	PACE_HIGH, CATEGORY_OVERSPENT, BUDGET_ALMOST_GONE, BUDGET_EXCEEDED, DAILY_ALLOWANCE
}

data class BudgetAlertDto(
	val id: UUID,
	val type: AlertType,
	val severity: AlertSeverity,
	val titleKey: String,
	val bodyKey: String,
	val params: Map<String, String>,
	val dailyRemaining: MoneyDto?,
	val category: PurchaseCategory?,
	val createdAt: Instant,
	val read: Boolean,
)

data class TripAlertsResponse(
	val alerts: List<BudgetAlertDto>,
)

data class TaxFreeRulesDto(
	val currency: String,
	val minimumPurchase: MoneyDto,
	val estimatedRefundRate: BigDecimal,
	val regionLabel: String,
)

data class TaxFreeEligibleItemDto(
	val purchaseId: UUID,
	val name: String,
	val amount: MoneyDto,
	val estimatedRefund: MoneyDto,
	val meetsMinimum: Boolean,
)

data class TaxFreeSummaryDto(
	val tripId: UUID,
	val rules: TaxFreeRulesDto,
	val eligibleCount: Int,
	val eligibleTotal: MoneyDto,
	val estimatedRefundTotal: MoneyDto,
	val remainingToMinimum: MoneyDto?,
	val items: List<TaxFreeEligibleItemDto>,
)

data class GeoPointDto(
	val lat: BigDecimal?,
	val lng: BigDecimal?,
)

data class RouteStopDto(
	val id: UUID,
	val title: String,
	val place: String?,
	val date: LocalDate,
	val amountSpentHere: MoneyDto,
	val point: GeoPointDto?,
	val orderIndex: Int,
)

data class TripRouteDto(
	val tripId: UUID,
	val stopCount: Int,
	val distanceMeters: BigDecimal?,
	val stops: List<RouteStopDto>,
	val path: List<GeoPointDto>,
)
