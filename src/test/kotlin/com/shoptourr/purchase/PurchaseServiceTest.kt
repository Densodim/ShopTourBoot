package com.shoptourr.purchase

import com.shoptourr.DomainValidationException
import com.shoptourr.purchase.dto.CreatePurchaseRequest
import com.shoptourr.purchase.dto.PurchaseCategory
import com.shoptourr.media.MediaService
import com.shoptourr.push.PushService
import com.shoptourr.shared.dto.MoneyDto
import com.shoptourr.trip.Traveler
import com.shoptourr.trip.Trip
import com.shoptourr.trip.TripService
import com.shoptourr.trip.dto.TripStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mock
import org.mockito.Mockito.lenient
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.data.domain.Pageable
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class PurchaseServiceTest {

	@Mock
	private lateinit var purchases: PurchaseRepository

	@Mock
	private lateinit var tripService: TripService

	@Mock
	private lateinit var mediaService: MediaService

	@Mock
	private lateinit var pushService: PushService

	private val clock = Clock.fixed(Instant.parse("2026-08-13T12:00:00Z"), ZoneOffset.UTC)
	private lateinit var service: PurchaseService
	private lateinit var trip: Trip
	private val ownerId = UUID.fromString("11111111-1111-1111-1111-111111111111")

	@BeforeEach
	fun setUp() {
		service = PurchaseService(purchases, tripService, mediaService, pushService, clock)
		trip = Trip(
			ownerId = ownerId,
			city = "Lisbon",
			country = "Portugal",
			status = TripStatus.ACTIVE.name,
			startDate = LocalDate.of(2026, 8, 10),
			endDate = LocalDate.of(2026, 8, 20),
			budgetAmount = BigDecimal("1500.00"),
			budgetCurrency = "EUR",
			defaultVatRatePercent = BigDecimal("23"),
			createdAt = Instant.now(clock),
			updatedAt = Instant.now(clock),
		)
		trip.fxRate = BigDecimal.ONE
		trip.fxQuoteCurrency = "EUR"
		trip.travelers.add(
			Traveler(
				trip = trip,
				userId = ownerId,
				name = "Ada",
				colorHex = "#FFD84D",
				isOwner = true,
				createdAt = Instant.now(clock),
			),
		)
		lenient().`when`(tripService.requireOwned(ownerId, trip.id)).thenReturn(trip)
		lenient().`when`(purchases.save(any(Purchase::class.java))).thenAnswer { it.arguments[0] }
		lenient().`when`(purchases.findAllByTripIdAndDeletedAtIsNullOrderByPurchaseDateDescPurchaseTimeDesc(trip.id))
			.thenReturn(emptyList())
		lenient().`when`(
			purchases.findByTripIdAndDeletedAtIsNullOrderByPurchaseDateDescIdDesc(eq(trip.id) ?: trip.id, any(Pageable::class.java) ?: Pageable.unpaged()),
		).thenReturn(emptyList())
		lenient().`when`(purchases.sumGrossByTripId(trip.id)).thenReturn(BigDecimal.ZERO)
	}

	@Test
	fun `create uses trip VAT and stores gross`() {
		val dto = service.create(
			ownerId,
			trip.id,
			CreatePurchaseRequest(
				name = "Pastel de nata",
				category = PurchaseCategory.FOOD,
				amount = MoneyDto(BigDecimal("123.00"), "EUR"),
				vatIncluded = true,
			),
		)

		assertEquals(BigDecimal("123.00"), dto.amount.amount)
		assertEquals(BigDecimal("100.00"), dto.vat.net)
		assertEquals(BigDecimal("23.00"), dto.vat.vat)
		assertEquals("today", PurchaseService.labelKey(dto.purchaseDate, LocalDate.now(clock)))
		assertEquals(1, dto.splitWithTravelerIds.size)
	}

	@Test
	fun `list groups purchases by day`() {
		val item = Purchase(
			tripId = trip.id,
			name = "Coffee",
			category = "FOOD",
			grossAmount = BigDecimal("4.50"),
			currency = "EUR",
			netAmount = BigDecimal("3.66"),
			vatAmount = BigDecimal("0.84"),
			vatRatePercent = BigDecimal("23"),
			vatIncluded = true,
			purchaseDate = LocalDate.of(2026, 8, 13),
			purchaseTime = java.time.LocalTime.of(10, 0),
			createdAt = Instant.now(clock),
			updatedAt = Instant.now(clock),
		)
		item.splitTravelerIds.add(trip.travelers.first().id)
		`when`(
			purchases.findByTripIdAndDeletedAtIsNullOrderByPurchaseDateDescIdDesc(eq(trip.id) ?: trip.id, any(Pageable::class.java) ?: Pageable.unpaged()),
		).thenReturn(listOf(item))
		`when`(purchases.sumGrossByTripId(trip.id)).thenReturn(BigDecimal("4.50"))

		val list = service.list(ownerId, trip.id)

		assertEquals(BigDecimal("4.50"), list.spentTotal.amount)
		assertEquals(1, list.days.size)
		assertEquals("today", list.days.single().labelKey)
	}

	@Test
	fun `list includes receipt thumbnail when media is ready`() {
		val mediaId = UUID.fromString("33333333-3333-3333-3333-333333333333")
		val item = Purchase(
			tripId = trip.id,
			name = "Coffee",
			category = "FOOD",
			grossAmount = BigDecimal("4.50"),
			currency = "EUR",
			netAmount = BigDecimal("3.66"),
			vatAmount = BigDecimal("0.84"),
			vatRatePercent = BigDecimal("23"),
			vatIncluded = true,
			purchaseDate = LocalDate.of(2026, 8, 13),
			purchaseTime = java.time.LocalTime.of(10, 0),
			receiptMediaId = mediaId,
			createdAt = Instant.now(clock),
			updatedAt = Instant.now(clock),
		)
		item.splitTravelerIds.add(trip.travelers.first().id)
		`when`(
			purchases.findByTripIdAndDeletedAtIsNullOrderByPurchaseDateDescIdDesc(eq(trip.id) ?: trip.id, any(Pageable::class.java) ?: Pageable.unpaged()),
		).thenReturn(listOf(item))
		`when`(purchases.sumGrossByTripId(trip.id)).thenReturn(BigDecimal("4.50"))
		`when`(mediaService.publicUrlIfReady(ownerId, mediaId))
			.thenReturn("http://localhost:8080/dev-uploads/$mediaId")

		val list = service.list(ownerId, trip.id)

		assertEquals("http://localhost:8080/dev-uploads/$mediaId", list.days.single().items.single().receiptThumbnailUrl)
	}

	@Test
	fun `create notifies when spend crosses 80 percent of budget`() {
		val prior = Purchase(
			tripId = trip.id,
			name = "Hotel",
			category = "HOTEL",
			grossAmount = BigDecimal("1100.00"),
			currency = "EUR",
			netAmount = BigDecimal("894.31"),
			vatAmount = BigDecimal("205.69"),
			vatRatePercent = BigDecimal("23"),
			vatIncluded = true,
			purchaseDate = LocalDate.of(2026, 8, 12),
			purchaseTime = java.time.LocalTime.of(9, 0),
			createdAt = Instant.now(clock),
			updatedAt = Instant.now(clock),
		)
		`when`(purchases.findAllByTripIdAndDeletedAtIsNullOrderByPurchaseDateDescPurchaseTimeDesc(trip.id))
			.thenReturn(listOf(prior))

		service.create(
			ownerId,
			trip.id,
			CreatePurchaseRequest(
				name = "Dinner",
				category = PurchaseCategory.FOOD,
				amount = MoneyDto(BigDecimal("150.00"), "EUR"),
				vatIncluded = true,
			),
		)

		org.mockito.Mockito.verify(pushService).notifyBudgetCrossing(
			ownerId,
			trip.id,
			BigDecimal("1100.00"),
			BigDecimal("1250.00"),
			BigDecimal("1500.00"),
		)
	}

	@Test
	fun `list spent total uses the trip sum not the page`() {
		val pageItem = listedPurchase(
			id = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
			date = LocalDate.of(2026, 8, 13),
			gross = "4.50",
		)
		`when`(
			purchases.findByTripIdAndDeletedAtIsNullOrderByPurchaseDateDescIdDesc(
				eq(trip.id) ?: trip.id,
				any(Pageable::class.java) ?: Pageable.unpaged(),
			),
		).thenReturn(listOf(pageItem))
		`when`(purchases.sumGrossByTripId(trip.id)).thenReturn(BigDecimal("99.00"))

		val list = service.list(ownerId, trip.id, size = 1)

		assertEquals(BigDecimal("99.00"), list.spentTotal.amount)
		assertEquals(1, list.days.single().items.size)
	}

	@Test
	fun `list after cursor asks for the next keyset page`() {
		val cursorId = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb")
		val cursorDate = LocalDate.of(2026, 8, 13)
		val older = listedPurchase(
			id = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc"),
			date = LocalDate.of(2026, 8, 12),
			gross = "3.00",
		)
		`when`(
			purchases.findPageAfterCursor(
				eq(trip.id) ?: trip.id,
				eq(cursorDate) ?: cursorDate,
				eq(cursorId) ?: cursorId,
				any(Pageable::class.java) ?: Pageable.unpaged(),
			),
		).thenReturn(listOf(older))
		`when`(purchases.sumGrossByTripId(trip.id)).thenReturn(BigDecimal("7.50"))

		val list = service.list(ownerId, trip.id, afterDate = cursorDate, afterId = cursorId, size = 2)

		assertEquals("2026-08-12", list.days.single().date.toString())
		assertEquals(older.id, list.days.single().items.single().id)
	}

	@Test
	fun `list rejects a cursor with only one half`() {
		assertThrows<DomainValidationException> {
			service.list(ownerId, trip.id, afterDate = LocalDate.of(2026, 8, 13), afterId = null)
		}
	}

	@Test
	fun `list rejects a size outside 1 to 100`() {
		assertThrows<DomainValidationException> {
			service.list(ownerId, trip.id, size = 0)
		}
	}

	private fun listedPurchase(id: UUID, date: LocalDate, gross: String): Purchase {
		val amount = BigDecimal(gross)
		val item = Purchase(
			id = id,
			tripId = trip.id,
			name = "Item",
			category = "FOOD",
			grossAmount = amount,
			currency = "EUR",
			netAmount = amount,
			vatAmount = BigDecimal.ZERO,
			vatRatePercent = BigDecimal("23"),
			vatIncluded = true,
			purchaseDate = date,
			purchaseTime = java.time.LocalTime.of(10, 0),
			createdAt = Instant.now(clock),
			updatedAt = Instant.now(clock),
		)
		item.splitTravelerIds.add(trip.travelers.first().id)
		return item
	}
}
