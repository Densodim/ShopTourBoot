package com.shoptourr.identity

import com.shoptourr.identity.dto.ForgotPasswordRequest
import com.shoptourr.identity.dto.LoginRequest
import com.shoptourr.identity.dto.RefreshTokenRequest
import com.shoptourr.identity.dto.RegisterRequest
import com.shoptourr.identity.dto.ResetPasswordRequest
import io.valix.generated.ValixRegistry
import io.valix.spring.ValixFrameworkValidator
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Guards the auth DTOs against Valix's fail-open behaviour: every path that cannot find a
 * validator reports the payload as valid rather than failing, so a broken codegen pipeline
 * disables validation without a single log line. These tests go through the generated
 * `ValixRegistry` — the same entry point the argument resolver uses — so that failure mode
 * breaks the build instead.
 *
 * That every `@ValidValix` type reaches a validator at all is covered by
 * [com.shoptourr.web.ValixRegistryCoverageTest].
 */
class AuthDtoValidationTest {

	@Test
	fun `register request rejects a short password`() {
		val result = ValixRegistry.validate(
			RegisterRequest(displayName = "Ada", email = "ada@example.com", password = "ab"),
		)

		assertFalse(result.valid)
		assertEquals(listOf("password"), result.errors.map { it.field })
	}

	@Test
	fun `register request rejects a malformed email and a blank display name`() {
		val result = ValixRegistry.validate(
			RegisterRequest(displayName = "   ", email = "not-an-email", password = "secret1"),
		)

		assertFalse(result.valid)
		assertTrue(result.errors.map { it.field }.containsAll(listOf("displayName", "email")))
	}

	@Test
	fun `a valid register request passes`() {
		val result = ValixRegistry.validate(
			RegisterRequest(displayName = "Ada", email = "ada@example.com", password = "secret1", locale = "en"),
		)

		assertTrue(result.valid, "unexpected errors: ${result.errors.map { "${it.field}:${it.code}" }}")
	}

	@Test
	fun `an absent locale is valid, matching the nullable jakarta semantics it replaced`() {
		val result = ValixRegistry.validate(
			RegisterRequest(displayName = "Ada", email = "ada@example.com", password = "secret1", locale = null),
		)

		assertTrue(result.valid)
	}

	@Test
	fun `login, refresh, forgot and reset requests all reject invalid input`() {
		val invalid = listOf(
			LoginRequest(email = "nope", password = ""),
			RefreshTokenRequest(refreshToken = " "),
			ForgotPasswordRequest(email = "nope"),
			ResetPasswordRequest(email = "nope", token = "short", newPassword = "ab"),
		)

		invalid.forEach { request ->
			assertFalse(
				ValixRegistry.validate(request).valid,
				"${request::class.simpleName} validated as valid",
			)
		}
	}

	/** House rule: a raw password or token never leaves the process, not even inside an error. */
	@Test
	fun `secrets are masked in the rejected value`() {
		val errors = ValixRegistry.validate(
			ResetPasswordRequest(email = "ada@example.com", token = "short", newPassword = "ab"),
		).errors

		assertEquals(setOf("token", "newPassword"), errors.map { it.field }.toSet())
		assertTrue(errors.all { it.rejectedValue == "********" }, "unmasked: ${errors.map { it.rejectedValue }}")
	}

	/**
	 * Pins the upstream defect the local wiring exists to work around, so an upgrade that fixes it
	 * shows up here as a failing test rather than going unnoticed. See
	 * [com.shoptourr.web.ValixValidationConfig].
	 */
	@Test
	fun `valix-spring's own framework validator is still inert and must not be used`() {
		val result = ValixFrameworkValidator.validate(
			RegisterRequest(displayName = "", email = "not-an-email", password = "ab"),
		)

		assertTrue(
			result.valid,
			"valix-spring now validates correctly — drop ValixValidationConfig and re-enable " +
				"ValixSpringAutoConfiguration, keeping ApiExceptionHandler ahead of its advice.",
		)
	}
}
