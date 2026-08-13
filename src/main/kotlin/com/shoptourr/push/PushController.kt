package com.shoptourr.push

import com.shoptourr.identity.userId
import com.shoptourr.push.dto.DeviceDto
import com.shoptourr.push.dto.RegisterDeviceRequest
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.net.URI
import java.util.UUID

@RestController
@RequestMapping("/api/me/devices")
class PushController(
	private val pushService: PushService,
) {

	@PostMapping
	fun register(
		@AuthenticationPrincipal jwt: Jwt,
		@Valid @RequestBody request: RegisterDeviceRequest,
	): ResponseEntity<DeviceDto> {
		val body = pushService.register(jwt.userId(), request)
		return ResponseEntity.created(URI.create("/api/me/devices/${body.id}")).body(body)
	}

	@DeleteMapping("/{deviceId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	fun unregister(
		@AuthenticationPrincipal jwt: Jwt,
		@PathVariable deviceId: UUID,
	) {
		pushService.unregister(jwt.userId(), deviceId)
	}
}
