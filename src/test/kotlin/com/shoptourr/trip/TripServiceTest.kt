package com.shoptourr.trip

import com.shoptourr.DomainValidationException
import com.shoptourr.ResourceNotFoundException
import com.shoptourr.identity.AppUser
import com.shoptourr.identity.AppUserRepository
import com.shoptourr.purchase.PurchaseRepository
import com.shoptourr.shared.dto.MoneyDto
import com.shoptourr.trip.dto.CreateTripRequest
import com.shoptourr.trip.dto.TripStatus
import com.shoptourr.trip.dto.UpdateTripRequest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.any
import org.mockito.Mock
import org.mockito.Mockito.lenient
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Optional
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class TripServiceTest {

	@Mock
	private lateinit var trips: TripRepository

	@Mock
	private lateinit var users: AppUserRepository

	@Mock
	private lateinit var purchaseRepo: PurchaseRepository

	@Mock
	private lateinit var invites: TripInviteRepository

	@Mock
	private lateinit var fxRates: com.shoptourr.fx.FxRateService

	private val clock = Clock.fixed(Instant.parse("2026-08-13T12:00:00Z"), ZoneOffset.UTC)
	private lateinit var service: TripService
	private lateinit var owner: AppUser

	@BeforeEach
	fun setUp() {
		service = TripService(trips, users, clock, purchaseRepo, invites, fxRates)
		owner = AppUser(
			email = "ada@example.com",
			passwordHash = "hash",
			displayName = "Ada",
			preferredCurrency = "RUB",
			createdAt = Instant.now(clock),
			updatedAt = Instant.now(clock),
		)
		lenient().`when`(users.findById(owner.id)).thenReturn(Optional.of(owner))
		lenient().`when`(trips.save(any(Trip::class.java))).thenAnswer { it.arguments[0] }
		lenient().`when`(purchaseRepo.sumGrossByTripId(any() ?: UUID.randomUUID())).thenReturn(BigDecimal.ZERO)
		lenient().`when`(purchaseRepo.countByTripIdAndDeletedAtIsNull(any() ?: UUID.randomUUID())).thenReturn(0)
		lenient().`when`(fxRates.quote(any() ?: "EUR", any() ?: "RUB")).thenAnswer { invocation ->
			FxCatalog.quote(invocation.getArgument(0), invocation.getArgument(1))
		}
	}

	@Test
	fun `create stores an upcoming trip with the owner as traveler`() {
		val result = service.create(owner.id, request(start = LocalDate.of(2026, 9, 1), end = LocalDate.of(2026, 9, 8)))

		assertEquals("Lisbon", result.city)
		assertEquals(TripStatus.UPCOMING, result.status)
		assertEquals("🇵🇹", result.flagEmoji)
		assertEquals("1–8 SEP", result.datesLabel)
		assertEquals(8, result.dayCount)
		assertEquals(1, result.travelers.size)
		assertTrue(result.travelers.single().isOwner)
		assertEquals("Ada", result.travelers.single().name)
		assertEquals(0, result.spent.amount.compareTo(BigDecimal.ZERO))
		val fx = requireNotNull(result.exchangeRate)
		assertEquals("EUR", fx.tripCurrency)
		assertEquals("RUB", fx.quoteCurrency)
		assertEquals("catalog", fx.provider)
		assertEquals(0, fx.rate.compareTo(BigDecimal("95.000000")))
	}

	@Test
	fun `create rejects an end date before the start`() {
		assertThrows<DomainValidationException> {
			service.create(owner.id, request(start = LocalDate.of(2026, 9, 8), end = LocalDate.of(2026, 9, 1)))
		}
	}

	@Test
	fun `list buckets trips by computed status`() {
		val upcoming = persisted(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 8))
		val active = persisted(LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 20))
		val past = persisted(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 10))
		`when`(trips.findAllByOwnerIdAndDeletedAtIsNull(owner.id)).thenReturn(listOf(upcoming, active, past))

		val list = service.list(owner.id)

		assertEquals(listOf(active.id), list.active.map { it.id })
		assertEquals(listOf(upcoming.id), list.upcoming.map { it.id })
		assertEquals(listOf(past.id), list.past.map { it.id })
		assertEquals(4, list.active.single().currentDayNumber)
	}

	@Test
	fun `get hides another user's trip`() {
		val trip = persisted(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 8))
		`when`(trips.findByIdAndDeletedAtIsNull(trip.id)).thenReturn(trip)

		assertThrows<ResourceNotFoundException> {
			service.get(UUID.fromString("33333333-3333-3333-3333-333333333333"), trip.id)
		}
	}

	@Test
	fun `update changes the city`() {
		val trip = persisted(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 8))
		`when`(trips.findByIdAndDeletedAtIsNull(trip.id)).thenReturn(trip)

		val result = service.update(owner.id, trip.id, UpdateTripRequest(city = "Porto"))

		assertEquals("Porto", result.city)
	}

	@Test
	fun `delete sets deletedAt`() {
		val trip = persisted(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 8))
		`when`(trips.findByIdAndDeletedAtIsNull(trip.id)).thenReturn(trip)

		service.delete(owner.id, trip.id)

		assertEquals(Instant.now(clock), trip.deletedAt)
	}

	private fun request(start: LocalDate, end: LocalDate) = CreateTripRequest(
		city = "Lisbon",
		country = "Portugal",
		countryCode = "PT",
		startDate = start,
		endDate = end,
		budget = MoneyDto(BigDecimal("1500.00"), "EUR"),
	)

	private fun persisted(start: LocalDate, end: LocalDate): Trip {
		val trip = Trip(
			ownerId = owner.id,
			city = "Lisbon",
			country = "Portugal",
			countryCode = "PT",
			flagEmoji = "🇵🇹",
			status = TripStatus.UPCOMING.name,
			startDate = start,
			endDate = end,
			budgetAmount = BigDecimal("1500.00"),
			budgetCurrency = "EUR",
			createdAt = Instant.now(clock),
			updatedAt = Instant.now(clock),
		)
		trip.travelers.add(
			Traveler(
				trip = trip,
				userId = owner.id,
				name = "Ada",
				colorHex = TripService.OWNER_COLOR,
				isOwner = true,
				createdAt = Instant.now(clock),
			),
		)
		return trip
	}
}
