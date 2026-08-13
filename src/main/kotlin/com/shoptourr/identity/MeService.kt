package com.shoptourr.identity

import com.shoptourr.ResourceNotFoundException
import com.shoptourr.config.ClientProperties
import com.shoptourr.identity.dto.ActivatePremiumRequest
import com.shoptourr.identity.dto.ClientRemoteConfigDto
import com.shoptourr.identity.dto.FeatureFlagsDto
import com.shoptourr.identity.dto.PremiumPlan
import com.shoptourr.identity.dto.ThemePreference
import com.shoptourr.identity.dto.UpdatePreferencesRequest
import com.shoptourr.identity.dto.UpdateProfileRequest
import com.shoptourr.identity.dto.UserDto
import com.shoptourr.identity.dto.UserPreferencesDto
import com.shoptourr.identity.dto.UserStatsDto
import com.shoptourr.trip.TripService
import com.shoptourr.wishlist.WishlistService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Instant
import java.util.UUID

@Service
class MeService(
	private val users: AppUserRepository,
	private val clientProperties: ClientProperties,
	private val clock: Clock,
	private val tripService: TripService,
	private val wishlistService: WishlistService,
) {

	@Transactional(readOnly = true)
	fun getMe(userId: UUID): UserDto {
		val user = requireLiveUser(userId)
		val (tripsCount, countriesCount) = tripService.countsFor(userId)
		return user.toDto(tripsCount, countriesCount, wishlistService.countFor(userId))
	}

	@Transactional
	fun updateProfile(userId: UUID, request: UpdateProfileRequest): UserDto {
		val user = requireLiveUser(userId)
		user.displayName = request.displayName.trim()
		user.avatarMediaId = request.avatarMediaId
		user.updatedAt = Instant.now(clock)
		val (tripsCount, countriesCount) = tripService.countsFor(userId)
		return user.toDto(tripsCount, countriesCount, wishlistService.countFor(userId))
	}

	@Transactional
	fun activatePremium(userId: UUID, request: ActivatePremiumRequest): UserDto {
		val user = requireLiveUser(userId)
		user.premiumPlan = request.plan.name
		user.updatedAt = Instant.now(clock)
		val (tripsCount, countriesCount) = tripService.countsFor(userId)
		return user.toDto(tripsCount, countriesCount, wishlistService.countFor(userId))
	}

	@Transactional(readOnly = true)
	fun getPreferences(userId: UUID): UserPreferencesDto = requireLiveUser(userId).toPreferences()

	@Transactional
	fun updatePreferences(userId: UUID, request: UpdatePreferencesRequest): UserPreferencesDto {
		val user = requireLiveUser(userId)
		request.locale?.trim()?.takeIf { it.isNotBlank() }?.let { user.locale = it }
		request.preferredCurrency?.let { user.preferredCurrency = it }
		request.theme?.let { user.theme = it.name }
		request.pushNotificationsEnabled?.let { user.pushNotificationsEnabled = it }
		request.darkMode?.let { user.darkMode = it }
		user.updatedAt = Instant.now(clock)
		return user.toPreferences()
	}

	fun appConfig(): ClientRemoteConfigDto =
		ClientRemoteConfigDto(
			minAndroidBuild = clientProperties.minAndroidBuild,
			minIosBuild = clientProperties.minIosBuild,
			softMinAndroidBuild = clientProperties.softMinAndroidBuild,
			softMinIosBuild = clientProperties.softMinIosBuild,
			flags = FeatureFlagsDto(
				exportPdf = clientProperties.flags.exportPdf,
				ocrAssist = clientProperties.flags.ocrAssist,
				nativeMaps = clientProperties.flags.nativeMaps,
			),
			storeUrlAndroid = clientProperties.storeUrlAndroid,
			storeUrlIos = clientProperties.storeUrlIos,
		)

	private fun requireLiveUser(userId: UUID): AppUser {
		val user = users.findById(userId).orElse(null)
		if (user == null || user.deletedAt != null) {
			throw ResourceNotFoundException("User not found.")
		}
		return user
	}
}

fun AppUser.toDto(tripsCount: Int = 0, countriesCount: Int = 0, wishlistCount: Int = 0): UserDto =
	UserDto(
		id = id,
		displayName = displayName,
		email = email,
		avatarUrl = null,
		locale = locale,
		preferredCurrency = preferredCurrency,
		theme = ThemePreference.valueOf(theme),
		pushNotificationsEnabled = pushNotificationsEnabled,
		memberSince = createdAt,
		premiumPlan = PremiumPlan.valueOf(premiumPlan),
		stats = UserStatsDto(tripsCount = tripsCount, countriesCount = countriesCount, wishlistCount = wishlistCount),
	)

fun AppUser.toPreferences(): UserPreferencesDto =
	UserPreferencesDto(
		locale = locale,
		preferredCurrency = preferredCurrency,
		theme = ThemePreference.valueOf(theme),
		pushNotificationsEnabled = pushNotificationsEnabled,
		darkMode = darkMode,
	)
