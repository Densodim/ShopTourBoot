package com.shoptourr.trip

import com.shoptourr.identity.userId
import com.shoptourr.trip.dto.CreateTripRequest
import com.shoptourr.trip.dto.TripDto
import com.shoptourr.trip.dto.UpdateTripRequest
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.net.URI
import java.util.UUID

@RestController
@RequestMapping("/api/trips")
class TripController(
	private val tripService: TripService,
) {

	@GetMapping
	fun list(@AuthenticationPrincipal jwt: Jwt) = tripService.list(jwt.userId())

	@PostMapping
	fun create(
		@AuthenticationPrincipal jwt: Jwt,
		@Valid @RequestBody request: CreateTripRequest,
	): ResponseEntity<TripDto> {
		val body = tripService.create(jwt.userId(), request)
		return ResponseEntity.created(URI.create("/api/trips/${body.id}")).body(body)
	}

	@GetMapping("/{tripId}")
	fun get(
		@AuthenticationPrincipal jwt: Jwt,
		@PathVariable tripId: UUID,
	) = tripService.get(jwt.userId(), tripId)

	@PatchMapping("/{tripId}")
	fun update(
		@AuthenticationPrincipal jwt: Jwt,
		@PathVariable tripId: UUID,
		@Valid @RequestBody request: UpdateTripRequest,
	) = tripService.update(jwt.userId(), tripId, request)

	@DeleteMapping("/{tripId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	fun delete(
		@AuthenticationPrincipal jwt: Jwt,
		@PathVariable tripId: UUID,
	) {
		tripService.delete(jwt.userId(), tripId)
	}
}
