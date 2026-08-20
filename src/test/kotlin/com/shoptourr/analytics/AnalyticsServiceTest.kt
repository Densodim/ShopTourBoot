package com.shoptourr.analytics

import com.shoptourr.DomainValidationException
import com.shoptourr.analytics.dto.AnalyticsBatchRequest
import com.shoptourr.analytics.dto.AnalyticsEventIngestDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import tools.jackson.databind.json.JsonMapper
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class AnalyticsServiceTest {

	@Mock
	private lateinit var events: AnalyticsEventRepository

	private val clock = Clock.fixed(Instant.parse("2026-08-20T08:00:00Z"), ZoneOffset.UTC)
	private val json = JsonMapper.builder().build()
	private lateinit var service: AnalyticsService
	private val userId = UUID.fromString("11111111-1111-1111-1111-111111111111")

	@BeforeEach
	fun setUp() {
		service = AnalyticsService(events, json, clock)
	}

	@Test
	fun `ingest persists a new event`() {
		`when`(events.existsByUserIdAndClientEventId(userId, "e1")).thenReturn(false)
		`when`(events.save(org.mockito.ArgumentMatchers.any(AnalyticsEvent::class.java))).thenAnswer { it.arguments[0] }

		service.ingest(
			userId,
			AnalyticsBatchRequest(
				events = listOf(
					AnalyticsEventIngestDto(
						id = "e1",
						name = "home_opened",
						properties = mapOf("tab" to "home"),
						timestamp = "2026-08-20T07:59:00Z",
					),
				),
			),
		)

		val captor = ArgumentCaptor.forClass(AnalyticsEvent::class.java)
		verify(events).save(captor.capture())
		val saved = captor.value
		assertEquals(userId, saved.userId)
		assertEquals("e1", saved.clientEventId)
		assertEquals("home_opened", saved.name)
		assertEquals("""{"tab":"home"}""", saved.properties)
		assertEquals(Instant.parse("2026-08-20T07:59:00Z"), saved.occurredAt)
		assertEquals(Instant.parse("2026-08-20T08:00:00Z"), saved.receivedAt)
	}

	@Test
	fun `ingest skips a client event id that was already stored`() {
		`when`(events.existsByUserIdAndClientEventId(userId, "e1")).thenReturn(true)

		service.ingest(
			userId,
			AnalyticsBatchRequest(
				events = listOf(
					AnalyticsEventIngestDto(
						id = "e1",
						name = "home_opened",
						timestamp = "2026-08-20T07:59:00Z",
					),
				),
			),
		)

		verify(events, never()).save(org.mockito.ArgumentMatchers.any())
	}

	@Test
	fun `ingest rejects a malformed timestamp`() {
		assertThrows(DomainValidationException::class.java) {
			service.ingest(
				userId,
				AnalyticsBatchRequest(
					events = listOf(
						AnalyticsEventIngestDto(
							id = "e1",
							name = "home_opened",
							timestamp = "not-a-time",
						),
					),
				),
			)
		}
		verify(events, never()).save(org.mockito.ArgumentMatchers.any())
	}

	@Test
	fun `ingest count matches the batch size of new rows`() {
		`when`(events.existsByUserIdAndClientEventId(userId, "e1")).thenReturn(false)
		`when`(events.existsByUserIdAndClientEventId(userId, "e2")).thenReturn(true)
		`when`(events.save(org.mockito.ArgumentMatchers.any(AnalyticsEvent::class.java))).thenAnswer { it.arguments[0] }

		val stored = service.ingest(
			userId,
			AnalyticsBatchRequest(
				events = listOf(
					AnalyticsEventIngestDto("e1", "home_opened", timestamp = "2026-08-20T07:59:00Z"),
					AnalyticsEventIngestDto("e2", "export_tapped", timestamp = "2026-08-20T07:59:01Z"),
				),
			),
		)

		assertEquals(1, stored)
	}
}
