package com.shoptourr.push

import com.shoptourr.identity.AppUser
import com.shoptourr.identity.AppUserRepository
import com.shoptourr.identity.TokenService
import com.shoptourr.push.dto.PushPlatform
import com.shoptourr.push.dto.RegisterDeviceRequest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.Optional
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class PushServiceTest {

	@Mock
	private lateinit var devices: PushDeviceRepository

	@Mock
	private lateinit var users: AppUserRepository

	@Mock
	private lateinit var tokenService: TokenService

	@Mock
	private lateinit var fcm: LiveFcmClient

	private val clock = Clock.fixed(Instant.parse("2026-08-13T12:00:00Z"), ZoneOffset.UTC)
	private lateinit var service: PushService
	private val userId = UUID.fromString("11111111-1111-1111-1111-111111111111")
	private val tripId = UUID.fromString("22222222-2222-2222-2222-222222222222")

	@BeforeEach
	fun setUp() {
		service = PushService(devices, users, tokenService, fcm, clock)
	}

	@Test
	fun `register stores the live token`() {
		`when`(tokenService.hash("fcm-token")).thenReturn("hash-1")
		`when`(devices.findByUserIdAndTokenHashAndDeletedAtIsNull(userId, "hash-1")).thenReturn(null)
		`when`(devices.save(org.mockito.ArgumentMatchers.any(PushDevice::class.java))).thenAnswer { it.arguments[0] }

		val dto = service.register(userId, RegisterDeviceRequest("fcm-token", PushPlatform.ANDROID))

		assertEquals("hash-1".take(8), dto.tokenFingerprint)
		assertEquals(PushPlatform.ANDROID, dto.platform)
	}

	@Test
	fun `crossing prefers exceeded over almost gone`() {
		assertEquals(
			"BUDGET_EXCEEDED",
			PushService.crossing(BigDecimal("100"), BigDecimal("200"), BigDecimal("150")),
		)
		assertEquals(
			"BUDGET_ALMOST_GONE",
			PushService.crossing(BigDecimal("100"), BigDecimal("130"), BigDecimal("150")),
		)
		assertNull(PushService.crossing(BigDecimal("10"), BigDecimal("20"), BigDecimal("150")))
	}

	@Test
	fun `notify sends to devices and drops unregistered tokens`() {
		val user = AppUser(
			id = userId,
			email = "ada@example.com",
			passwordHash = "hash",
			displayName = "Ada",
			createdAt = Instant.now(clock),
			updatedAt = Instant.now(clock),
		)
		val device = PushDevice(
			userId = userId,
			tokenHash = "hash-1",
			token = "fcm-token",
			platform = "ANDROID",
			createdAt = Instant.now(clock),
			lastSeenAt = Instant.now(clock),
		)
		`when`(users.findById(userId)).thenReturn(Optional.of(user))
		`when`(devices.findAllByUserIdAndDeletedAtIsNull(userId)).thenReturn(listOf(device))
		`when`(
			fcm.send(
				org.mockito.ArgumentMatchers.eq("fcm-token") ?: "fcm-token",
				org.mockito.ArgumentMatchers.any() ?: "t",
				org.mockito.ArgumentMatchers.any() ?: "b",
				org.mockito.ArgumentMatchers.any() ?: emptyMap(),
			),
		).thenReturn(FcmSendResult.UNREGISTERED)

		service.notifyBudgetCrossing(userId, tripId, BigDecimal("100"), BigDecimal("200"), BigDecimal("150"))

		assertNotNull(device.deletedAt)
	}

	@Test
	fun `notify is skipped when the user disabled push`() {
		val user = AppUser(
			id = userId,
			email = "ada@example.com",
			passwordHash = "hash",
			displayName = "Ada",
			createdAt = Instant.now(clock),
			updatedAt = Instant.now(clock),
		)
		user.pushNotificationsEnabled = false
		`when`(users.findById(userId)).thenReturn(Optional.of(user))

		service.notifyBudgetCrossing(userId, tripId, BigDecimal("100"), BigDecimal("200"), BigDecimal("150"))

		verify(fcm, never()).send(
			org.mockito.ArgumentMatchers.any() ?: "t",
			org.mockito.ArgumentMatchers.any() ?: "t",
			org.mockito.ArgumentMatchers.any() ?: "b",
			org.mockito.ArgumentMatchers.any() ?: emptyMap(),
		)
	}
}
