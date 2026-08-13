package com.shoptourr.identity

import com.shoptourr.ResourceNotFoundException
import com.shoptourr.config.ClientProperties
import com.shoptourr.identity.dto.ThemePreference
import com.shoptourr.identity.dto.UpdatePreferencesRequest
import com.shoptourr.identity.dto.UpdateProfileRequest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.lenient
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import com.shoptourr.trip.TripService
import com.shoptourr.wishlist.WishlistService
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.Optional
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class MeServiceTest {

	@Mock
	private lateinit var users: AppUserRepository

	@Mock
	private lateinit var tripService: TripService

	@Mock
	private lateinit var wishlistService: WishlistService

	private val clock = Clock.fixed(Instant.parse("2026-08-13T12:00:00Z"), ZoneOffset.UTC)
	private lateinit var service: MeService
	private lateinit var user: AppUser

	@BeforeEach
	fun setUp() {
		service = MeService(users, ClientProperties(minAndroidBuild = 12, minIosBuild = 34), clock, tripService, wishlistService)
		user = AppUser(
			email = "ada@example.com",
			passwordHash = "hash",
			displayName = "Ada",
			locale = "en",
			createdAt = Instant.parse("2026-01-01T00:00:00Z"),
			updatedAt = Instant.parse("2026-01-01T00:00:00Z"),
		)
		lenient().`when`(users.findById(user.id)).thenReturn(Optional.of(user))
		lenient().`when`(tripService.countsFor(user.id)).thenReturn(0 to 0)
		lenient().`when`(wishlistService.countFor(user.id)).thenReturn(0)
	}

	@Test
	fun `getMe maps profile fields and empty stats`() {
		val dto = service.getMe(user.id)

		assertEquals("Ada", dto.displayName)
		assertEquals("ada@example.com", dto.email)
		assertEquals("en", dto.locale)
		assertEquals("RUB", dto.preferredCurrency)
		assertEquals(ThemePreference.SYSTEM, dto.theme)
		assertNull(dto.avatarUrl)
		assertEquals(0, dto.stats.tripsCount)
		assertEquals(user.createdAt, dto.memberSince)
	}

	@Test
	fun `updateProfile changes the display name`() {
		val dto = service.updateProfile(user.id, UpdateProfileRequest("Ada Lovelace"))

		assertEquals("Ada Lovelace", dto.displayName)
		assertEquals(Instant.now(clock), user.updatedAt)
	}

	@Test
	fun `updatePreferences applies only provided fields`() {
		val dto = service.updatePreferences(
			user.id,
			UpdatePreferencesRequest(preferredCurrency = "EUR", theme = ThemePreference.DARK),
		)

		assertEquals("en", dto.locale)
		assertEquals("EUR", dto.preferredCurrency)
		assertEquals(ThemePreference.DARK, dto.theme)
		assertFalse(user.darkMode)
	}

	@Test
	fun `activatePremium updates the plan`() {
		val dto = service.activatePremium(user.id, com.shoptourr.identity.dto.ActivatePremiumRequest(com.shoptourr.identity.dto.PremiumPlan.PLUS))

		assertEquals(com.shoptourr.identity.dto.PremiumPlan.PLUS, dto.premiumPlan)
		assertEquals("PLUS", user.premiumPlan)
	}

	@Test
	fun `missing user is not found`() {
		val missing = UUID.fromString("22222222-2222-2222-2222-222222222222")
		`when`(users.findById(missing)).thenReturn(Optional.empty())

		assertThrows<ResourceNotFoundException> { service.getMe(missing) }
	}

	@Test
	fun `appConfig comes from client properties`() {
		val config = service.appConfig()

		assertEquals(12, config.minAndroidBuild)
		assertEquals(34, config.minIosBuild)
		assertEquals(true, config.flags.exportPdf)
	}
}
