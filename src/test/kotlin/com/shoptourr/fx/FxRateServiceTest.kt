package com.shoptourr.fx

import com.shoptourr.config.FxProperties
import com.shoptourr.trip.FxCatalog
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
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
class FxRateServiceTest {

	@Mock
	private lateinit var live: LiveFxClient

	@Mock
	private lateinit var redis: StringRedisTemplate

	@Mock
	private lateinit var values: ValueOperations<String, String>

	private lateinit var service: FxRateService

	@BeforeEach
	fun setUp() {
		org.mockito.Mockito.lenient().`when`(redis.opsForValue()).thenReturn(values)
		service = FxRateService(live, redis, FxProperties())
	}

	@Test
	fun `same currency stays identity and skips the network`() {
		val quote = service.quote("eur", "EUR")

		assertEquals(FxCatalog.IDENTITY_PROVIDER, quote.provider)
		verify(live, never()).rates("EUR")
	}

	@Test
	fun `a live hit is preferred over the catalog`() {
		`when`(values.get("fx:live:EUR")).thenReturn(null)
		`when`(live.rates("EUR")).thenReturn(mapOf("RUB" to BigDecimal("101.25")))

		val quote = service.quote("EUR", "RUB")

		assertEquals("live", quote.provider)
		assertEquals(0, quote.rate.compareTo(BigDecimal("101.250000")))
		verify(values).set("fx:live:EUR", "RUB:101.25", Duration.ofHours(1))
	}

	@Test
	fun `cached rates skip the live client`() {
		`when`(values.get("fx:live:EUR")).thenReturn("RUB:99.5")

		val quote = service.quote("EUR", "RUB")

		assertEquals("live", quote.provider)
		assertEquals(0, quote.rate.compareTo(BigDecimal("99.500000")))
		verify(live, never()).rates("EUR")
	}

	@Test
	fun `live miss falls back to the catalog`() {
		`when`(values.get("fx:live:EUR")).thenReturn(null)
		`when`(live.rates("EUR")).thenReturn(null)

		val quote = service.quote("EUR", "RUB")

		assertEquals(FxCatalog.PROVIDER, quote.provider)
		assertEquals(0, quote.rate.compareTo(BigDecimal("95.000000")))
	}

	@Test
	fun `encode round-trips a rate map`() {
		val raw = FxRateService.encode(mapOf("USD" to BigDecimal("1.08"), "RUB" to BigDecimal("95")))
		val decoded = FxRateService.decode(raw)

		assertTrue(decoded.containsKey("USD"))
		assertEquals(0, decoded.getValue("RUB").compareTo(BigDecimal("95")))
	}
}
