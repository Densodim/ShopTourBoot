package com.shoptourr.geo

import com.shoptourr.config.GeoProperties
import com.shoptourr.insights.PlaceCatalog
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.ValueOperations
import java.math.BigDecimal
import java.time.Duration

@ExtendWith(MockitoExtension::class)
class GeocodeServiceTest {

	@Mock
	private lateinit var live: LiveGeocodeClient

	@Mock
	private lateinit var redis: StringRedisTemplate

	@Mock
	private lateinit var values: ValueOperations<String, String>

	private lateinit var service: GeocodeService

	@BeforeEach
	fun setUp() {
		org.mockito.Mockito.lenient().`when`(redis.opsForValue()).thenReturn(values)
		service = GeocodeService(live, redis, GeoProperties())
	}

	@Test
	fun `a live hit is cached and returned`() {
		val query = "Time Out Market, Lisbon, Portugal"
		`when`(values.get("geo:live:${query.lowercase()}")).thenReturn(null)
		`when`(live.search(query)).thenReturn(PlaceCatalog.LatLng(BigDecimal("38.700000"), BigDecimal("-9.140000")))

		val point = service.resolve("Time Out Market", "Lisbon", "Portugal", "PT")

		requireNotNull(point)
		assertEquals(0, point.lat.compareTo(BigDecimal("38.700000")))
		verify(values).set("geo:live:${query.lowercase()}", "38.700000,-9.140000", Duration.ofDays(7))
	}

	@Test
	fun `cache skips the live client`() {
		val query = "Time Out Market, Lisbon, Portugal"
		`when`(values.get("geo:live:${query.lowercase()}")).thenReturn("38.706900,-9.145700")

		val point = service.resolve("Time Out Market", "Lisbon", "Portugal", "PT")

		requireNotNull(point)
		assertEquals(0, point.lng.compareTo(BigDecimal("-9.145700")))
		verify(live, never()).search(query)
	}

	@Test
	fun `live miss falls back to the catalog`() {
		val query = "Time Out Market, Lisbon, Portugal"
		`when`(values.get("geo:live:${query.lowercase()}")).thenReturn(null)
		`when`(live.search(query)).thenReturn(null)

		val point = service.resolve("Time Out Market", "Lisbon", "Portugal", "PT")

		requireNotNull(point)
		assertEquals(0, point.lat.compareTo(BigDecimal("38.706900")))
	}

	@Test
	fun `unknown place without a live hit is absent`() {
		val query = "???, Nowhereville, Narnia"
		`when`(values.get("geo:live:${query.lowercase()}")).thenReturn(null)
		`when`(live.search(query)).thenReturn(null)

		assertNull(service.resolve("???", "Nowhereville", "Narnia", "ZZ"))
	}
}
