package com.shoptourr.identity

import com.shoptourr.identity.dto.ActivatePremiumRequest
import com.shoptourr.identity.dto.UpdatePreferencesRequest
import com.shoptourr.identity.dto.UpdateProfileRequest
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/me")
class MeController(
	private val meService: MeService,
) {

	@GetMapping
	fun me(@AuthenticationPrincipal jwt: Jwt) = meService.getMe(jwt.userId())

	@PatchMapping
	fun updateProfile(
		@AuthenticationPrincipal jwt: Jwt,
		@Valid @RequestBody request: UpdateProfileRequest,
	) = meService.updateProfile(jwt.userId(), request)

	@GetMapping("/preferences")
	fun preferences(@AuthenticationPrincipal jwt: Jwt) = meService.getPreferences(jwt.userId())

	@PatchMapping("/preferences")
	fun updatePreferences(
		@AuthenticationPrincipal jwt: Jwt,
		@Valid @RequestBody request: UpdatePreferencesRequest,
	) = meService.updatePreferences(jwt.userId(), request)

	@GetMapping("/app-config")
	fun appConfig() = meService.appConfig()

	@PostMapping("/premium/activate")
	fun activatePremium(
		@AuthenticationPrincipal jwt: Jwt,
		@Valid @RequestBody request: ActivatePremiumRequest,
	) = meService.activatePremium(jwt.userId(), request)

	@DeleteMapping
	@ResponseStatus(HttpStatus.NO_CONTENT)
	fun deleteMe(@AuthenticationPrincipal jwt: Jwt) {
		meService.deleteAccount(jwt.userId())
	}
}

fun Jwt.userId(): UUID = UUID.fromString(subject)
