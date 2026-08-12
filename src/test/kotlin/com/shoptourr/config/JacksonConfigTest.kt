package com.shoptourr.config

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.json.JsonTest
import org.springframework.context.annotation.Import
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import tools.jackson.databind.json.JsonMapper
import java.math.BigDecimal
import java.time.Instant

/**
 * Pins the wire format the API contract depends on. The `ProblemDetail` case is the reason this
 * config customizes Boot's mapper rather than replacing it — a replacement drops Spring's
 * `ProblemDetail` mixin and nests `code` inside a `properties` object.
 */
@JsonTest
@Import(JacksonConfig::class)
class JacksonConfigTest {

	private data class Payload(
		val amount: BigDecimal,
		val occurredAt: Instant,
	)

	@Autowired
	private lateinit var jsonMapper: JsonMapper

	@Test
	fun `money is serialized as a string to preserve scale`() {
		val json = jsonMapper.writeValueAsString(Payload(BigDecimal("96.50"), Instant.EPOCH))

		assertTrue(json.contains("\"amount\":\"96.50\""), json)
	}

	@Test
	fun `timestamps are serialized as ISO-8601, not epoch numbers`() {
		val json = jsonMapper.writeValueAsString(
			Payload(BigDecimal("1.00"), Instant.parse("2026-08-12T10:15:30Z")),
		)

		assertTrue(json.contains("\"occurredAt\":\"2026-08-12T10:15:30Z\""), json)
	}

	@Test
	fun `unknown properties are ignored so clients can send extra fields`() {
		val payload = jsonMapper.readValue(
			"""{"amount":"12.30","occurredAt":"2026-08-12T10:15:30Z","surprise":true}""",
			Payload::class.java,
		)

		assertEquals(BigDecimal("12.30"), payload.amount)
	}

	@Test
	fun `problem detail properties are flattened to top level, not nested`() {
		val problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Nope")
		problem.setProperty("code", "CONFLICT")

		val json = jsonMapper.writeValueAsString(problem)

		assertTrue(json.contains("\"code\":\"CONFLICT\""), json)
		assertFalse(json.contains("\"properties\""), json)
	}

	@Test
	fun `problem detail omits empty fields`() {
		val json = jsonMapper.writeValueAsString(
			ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Nope"),
		)

		assertFalse(json.contains("\"instance\""), json)
	}
}
