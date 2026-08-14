package com.shoptourr.ratelimit

import com.shoptourr.config.RateLimitProperties
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.data.redis.RedisConnectionFailureException
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.ValueOperations
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset

@ExtendWith(MockitoExtension::class)
class RateLimitServiceTest {

	@Mock
	private lateinit var redis: StringRedisTemplate

	@Mock
	private lateinit var values: ValueOperations<String, String>

	private val clock = Clock.fixed(Instant.parse("2026-08-13T12:00:00Z"), ZoneOffset.UTC)
	private lateinit var service: RateLimitService

	@BeforeEach
	fun setUp() {
		service = RateLimitService(redis, RateLimitProperties(window = Duration.ofMinutes(1)), clock)
		org.mockito.Mockito.lenient().`when`(redis.opsForValue()).thenReturn(values)
	}

	@Test
	fun `first hit in a window is allowed and sets expiry`() {
		`when`(values.increment(anyString())).thenReturn(1L)

		val decision = service.consume("auth", "127.0.0.1", 20)

		assertTrue(decision.allowed)
		assertEquals(20, decision.limit)
		assertEquals(19, decision.remaining)
		verify(redis).expire("rl:auth:127.0.0.1:${clock.instant().epochSecond / 60}", Duration.ofMinutes(1))
	}

	@Test
	fun `overflow in the window is rejected`() {
		`when`(values.increment(anyString())).thenReturn(21L)

		val decision = service.consume("auth", "127.0.0.1", 20)

		assertFalse(decision.allowed)
		assertEquals(0, decision.remaining)
	}

	@Test
	fun `redis outage fails open`() {
		`when`(values.increment(anyString())).thenThrow(RedisConnectionFailureException("down"))

		val decision = service.consume("api", "127.0.0.1", 300)

		assertTrue(decision.allowed)
		assertEquals(300, decision.remaining)
	}
}
