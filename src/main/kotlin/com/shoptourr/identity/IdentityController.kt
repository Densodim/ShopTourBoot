package com.shoptourr.identity

import com.shoptourr.identity.dto.AuthTokensResponse
import com.shoptourr.identity.dto.ForgotPasswordRequest
import com.shoptourr.identity.dto.LoginRequest
import com.shoptourr.identity.dto.LogoutRequest
import com.shoptourr.identity.dto.RefreshTokenRequest
import com.shoptourr.identity.dto.RegisterRequest
import com.shoptourr.identity.dto.ResetPasswordRequest
import com.shoptourr.identity.dto.SocialLoginRequest
import io.valix.spring.ValidValix
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.net.URI
import java.util.UUID

@RestController
@RequestMapping("/api/auth")
class IdentityController(
	private val identityService: IdentityService,
) {

	@PostMapping("/register")
	fun register(@ValidValix @RequestBody request: RegisterRequest): ResponseEntity<AuthTokensResponse> {
		val body = identityService.register(request)
		return ResponseEntity.created(URI.create("/api/me")).body(body)
	}

	@PostMapping("/login")
	fun login(@ValidValix @RequestBody request: LoginRequest) = identityService.login(request)

	@PostMapping("/oauth")
	fun loginSocial(@ValidValix @RequestBody request: SocialLoginRequest) =
		identityService.loginSocial(request)

	@PostMapping("/refresh")
	fun refresh(@ValidValix @RequestBody request: RefreshTokenRequest) = identityService.refresh(request)

	@PostMapping("/logout")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	fun logout(
		@AuthenticationPrincipal jwt: Jwt,
		@RequestBody(required = false) request: LogoutRequest?,
	) {
		val userId = UUID.fromString(jwt.subject)
		val sessionId = jwt.getClaimAsString(TokenService.SESSION_CLAIM)?.let(UUID::fromString)
		identityService.logout(userId, sessionId, request)
	}

	@PostMapping("/forgot-password")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	fun forgotPassword(@ValidValix @RequestBody request: ForgotPasswordRequest) {
		identityService.forgotPassword(request)
	}

	@PostMapping("/reset-password")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	fun resetPassword(@ValidValix @RequestBody request: ResetPasswordRequest) {
		identityService.resetPassword(request)
	}
}
