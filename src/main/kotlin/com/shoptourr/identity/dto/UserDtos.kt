package com.shoptourr.identity.dto

import com.shoptourr.shared.validation.FieldPatterns
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

enum class ThemePreference {
	SYSTEM, LIGHT, DARK
}

enum class PremiumPlan {
	FREE, PLUS, PRO
}

data class UserStatsDto(
	val tripsCount: Int,
	val countriesCount: Int,
	val wishlistCount: Int,
)

data class UserDto(
	val id: UUID,
	val displayName: String,
	val email: String,
	val avatarUrl: String?,
	val locale: String,
	val preferredCurrency: String,
	val theme: ThemePreference,
	val pushNotificationsEnabled: Boolean,
	val memberSince: Instant,
	val premiumPlan: PremiumPlan,
	val stats: UserStatsDto,
)

data class ActivatePremiumRequest(
	@field:NotNull
	val plan: PremiumPlan,
)

data class UpdateProfileRequest(
	@field:NotBlank
	@field:Size(min = 2, max = 80)
	@field:Pattern(regexp = FieldPatterns.PERSON_OR_PLACE)
	val displayName: String,
	val avatarMediaId: UUID? = null,
)

data class UserPreferencesDto(
	val locale: String,
	val preferredCurrency: String,
	val theme: ThemePreference,
	val pushNotificationsEnabled: Boolean,
	val darkMode: Boolean,
)

data class UpdatePreferencesRequest(
	@field:Size(min = 2, max = 5)
	@field:Pattern(regexp = FieldPatterns.LOCALE)
	val locale: String? = null,
	@field:Size(min = 3, max = 3)
	@field:Pattern(regexp = FieldPatterns.ISO_4217)
	val preferredCurrency: String? = null,
	val theme: ThemePreference? = null,
	val pushNotificationsEnabled: Boolean? = null,
	val darkMode: Boolean? = null,
)

data class FeatureFlagsDto(
	val exportPdf: Boolean = true,
	val ocrAssist: Boolean = true,
	val nativeMaps: Boolean = false,
)

data class ClientRemoteConfigDto(
	val minAndroidBuild: Int,
	val minIosBuild: Int,
	val softMinAndroidBuild: Int? = null,
	val softMinIosBuild: Int? = null,
	val flags: FeatureFlagsDto = FeatureFlagsDto(),
	val storeUrlAndroid: String? = null,
	val storeUrlIos: String? = null,
)
