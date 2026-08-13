package com.shoptourr.idempotency

import com.shoptourr.DomainValidationException
import com.shoptourr.IdempotencyConflictException
import com.shoptourr.config.IdempotencyProperties
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class IdempotencyServiceTest {

	@Mock
	private lateinit var records: IdempotencyRecordRepository

	private val clock = Clock.fixed(Instant.parse("2026-08-13T12:00:00Z"), ZoneOffset.UTC)
	private lateinit var service: IdempotencyService
	private val userId = UUID.fromString("11111111-1111-1111-1111-111111111111")

	@BeforeEach
	fun setUp() {
		service = IdempotencyService(records, IdempotencyProperties(Duration.ofHours(24)), clock)
	}

	@Test
	fun `missing record is a miss`() {
		`when`(records.findByUserIdAndRouteKeyAndIdempotencyKey(userId, "POST /api/trips", "k1"))
			.thenReturn(null)

		assertNull(service.replayOrNull(userId, "POST /api/trips", "k1", "abc"))
	}

	@Test
	fun `same hash replays the stored response`() {
		`when`(records.findByUserIdAndRouteKeyAndIdempotencyKey(userId, "POST /api/trips", "k1"))
			.thenReturn(sample(hash = "abc"))

		val replay = service.replayOrNull(userId, "POST /api/trips", "k1", "abc")

		assertEquals(201, replay!!.status)
		assertEquals("""{"id":"t1"}""", replay.body)
	}

	@Test
	fun `different hash is an idempotency conflict`() {
		`when`(records.findByUserIdAndRouteKeyAndIdempotencyKey(userId, "POST /api/trips", "k1"))
			.thenReturn(sample(hash = "abc"))

		assertThrows<IdempotencyConflictException> {
			service.replayOrNull(userId, "POST /api/trips", "k1", "other")
		}
	}

	@Test
	fun `expired record is deleted and treated as a miss`() {
		val expired = sample(hash = "abc", expiresAt = Instant.parse("2026-08-12T12:00:00Z"))
		`when`(records.findByUserIdAndRouteKeyAndIdempotencyKey(userId, "POST /api/trips", "k1"))
			.thenReturn(expired)

		assertNull(service.replayOrNull(userId, "POST /api/trips", "k1", "abc"))
		verify(records).delete(expired)
	}

	@Test
	fun `server errors are not remembered`() {
		service.remember(userId, "POST /api/trips", "k1", "abc", 500, "{}")

		verify(records, never()).save(org.mockito.ArgumentMatchers.any() ?: sample("abc"))
	}

	@Test
	fun `key longer than 64 characters is rejected`() {
		assertThrows<DomainValidationException> {
			IdempotencyService.normalizeKey("k".repeat(65))
		}
	}

	private fun sample(
		hash: String,
		expiresAt: Instant = Instant.parse("2026-08-14T12:00:00Z"),
	) = IdempotencyRecord(
		userId = userId,
		routeKey = "POST /api/trips",
		idempotencyKey = "k1",
		requestHash = hash,
		responseStatus = 201,
		responseBody = """{"id":"t1"}""",
		createdAt = Instant.parse("2026-08-13T11:00:00Z"),
		expiresAt = expiresAt,
	)
}
