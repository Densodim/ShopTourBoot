package com.shoptourr.shared.validation

import com.shoptourr.diary.dto.CreateDiaryEntryRequest
import com.shoptourr.identity.dto.UpdatePreferencesRequest
import com.shoptourr.identity.dto.UpdateProfileRequest
import com.shoptourr.media.dto.CreateMediaUploadIntentRequest
import com.shoptourr.media.dto.MediaPurpose
import com.shoptourr.purchase.dto.CreatePurchaseRequest
import com.shoptourr.purchase.dto.PurchaseCategory
import com.shoptourr.shared.dto.MoneyDto
import com.shoptourr.trip.dto.CreateTripRequest
import com.shoptourr.trip.dto.InviteTravelerRequest
import com.shoptourr.wishlist.dto.CreateWishlistItemRequest
import jakarta.validation.Validation
import jakarta.validation.Validator
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RequestDtoValidationTest {

	private val validator: Validator = Validation.buildDefaultValidatorFactory().validator

	@Test
	fun `a well formed trip is accepted`() {
		assertTrue(validator.validate(validTrip()).isEmpty())
	}

	@Test
	fun `trip city and country reject symbol soup`() {
		assertFieldInvalid(validTrip().copy(city = "@@@"), "city")
		assertFieldInvalid(validTrip().copy(country = "!!!"), "country")
	}

	@Test
	fun `trip country code and quote currency must be uppercase ISO`() {
		assertFieldInvalid(validTrip().copy(countryCode = "pt"), "countryCode")
		assertTrue(validator.validate(validTrip().copy(countryCode = "PT")).isEmpty())
		assertFieldInvalid(validTrip().copy(quoteCurrency = "eur"), "quoteCurrency")
	}

	@Test
	fun `invite email must be an email not any string with an at-sign`() {
		assertFieldInvalid(InviteTravelerRequest(email = "not-an-email"), "email")
		assertFieldInvalid(InviteTravelerRequest(email = "friend@"), "email")
		assertTrue(validator.validate(InviteTravelerRequest(email = "friend@voyage.app")).isEmpty())
	}

	@Test
	fun `purchase name rejects tags`() {
		assertFieldInvalid(
			CreatePurchaseRequest(
				name = "<script>",
				category = PurchaseCategory.FOOD,
				amount = MoneyDto(BigDecimal("4.50"), "EUR"),
			),
			"name",
		)
	}

	@Test
	fun `wishlist city rejects digits-only input`() {
		assertFieldInvalid(
			CreateWishlistItemRequest(
				name = "Pastel",
				city = "123",
				targetPrice = MoneyDto(BigDecimal("10.00"), "EUR"),
			),
			"city",
		)
	}

	@Test
	fun `diary mood rejects punctuation-only tokens`() {
		assertFieldInvalid(
			CreateDiaryEntryRequest(mood = "!!!!!!!!", text = "Walked the city"),
			"mood",
		)
		assertTrue(
			validator.validate(CreateDiaryEntryRequest(mood = "good", text = "Walked the city")).isEmpty(),
		)
	}

	@Test
	fun `profile and preferences reject garbage locale and display name`() {
		assertFieldInvalid(UpdateProfileRequest(displayName = "@@@"), "displayName")
		assertFieldInvalid(UpdatePreferencesRequest(locale = "de"), "locale")
		assertFieldInvalid(UpdatePreferencesRequest(preferredCurrency = "eur"), "preferredCurrency")
		assertTrue(validator.validate(UpdatePreferencesRequest(locale = "ru", preferredCurrency = "EUR")).isEmpty())
	}

	@Test
	fun `media content type must be a receipt image`() {
		assertFieldInvalid(
			CreateMediaUploadIntentRequest(MediaPurpose.RECEIPT, "text/html", 1024),
			"contentType",
		)
		assertTrue(
			validator.validate(
				CreateMediaUploadIntentRequest(MediaPurpose.RECEIPT, "image/jpeg", 1024),
			).isEmpty(),
		)
	}

	private fun assertFieldInvalid(target: Any, field: String) {
		val paths = validator.validate(target).map { it.propertyPath.toString() }
		assertFalse(paths.isEmpty(), "expected $field to be invalid, but the payload was valid")
		assertTrue(
			paths.any { it == field || it.startsWith("$field.") || it.endsWith(field) },
			"expected $field in $paths",
		)
	}

	private fun validTrip() = CreateTripRequest(
		city = "Lisbon",
		country = "Portugal",
		countryCode = "PT",
		startDate = LocalDate.parse("2026-04-12"),
		endDate = LocalDate.parse("2026-04-19"),
		budget = MoneyDto(BigDecimal("1500.00"), "EUR"),
		quoteCurrency = "EUR",
	)
}
