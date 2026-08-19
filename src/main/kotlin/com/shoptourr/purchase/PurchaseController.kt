package com.shoptourr.purchase

import com.shoptourr.identity.userId
import com.shoptourr.purchase.dto.CreatePurchaseRequest
import com.shoptourr.purchase.dto.PurchaseDto
import com.shoptourr.purchase.dto.UpdatePurchaseRequest
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
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.net.URI
import java.time.LocalDate
import java.util.UUID

@RestController
@RequestMapping("/api/trips/{tripId}/purchases")
class PurchaseController(
	private val purchaseService: PurchaseService,
) {

	@GetMapping
	fun list(
		@AuthenticationPrincipal jwt: Jwt,
		@PathVariable tripId: UUID,
		@RequestParam(required = false) afterDate: LocalDate?,
		@RequestParam(required = false) afterId: UUID?,
		@RequestParam(defaultValue = "50") size: Int,
	) = purchaseService.list(jwt.userId(), tripId, afterDate, afterId, size)

	@PostMapping
	fun create(
		@AuthenticationPrincipal jwt: Jwt,
		@PathVariable tripId: UUID,
		@Valid @RequestBody request: CreatePurchaseRequest,
	): ResponseEntity<PurchaseDto> {
		val body = purchaseService.create(jwt.userId(), tripId, request)
		return ResponseEntity.created(URI.create("/api/trips/$tripId/purchases/${body.id}")).body(body)
	}

	@GetMapping("/{id}")
	fun get(
		@AuthenticationPrincipal jwt: Jwt,
		@PathVariable tripId: UUID,
		@PathVariable id: UUID,
	) = purchaseService.get(jwt.userId(), tripId, id)

	@PatchMapping("/{id}")
	fun update(
		@AuthenticationPrincipal jwt: Jwt,
		@PathVariable tripId: UUID,
		@PathVariable id: UUID,
		@Valid @RequestBody request: UpdatePurchaseRequest,
	) = purchaseService.update(jwt.userId(), tripId, id, request)

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	fun delete(
		@AuthenticationPrincipal jwt: Jwt,
		@PathVariable tripId: UUID,
		@PathVariable id: UUID,
	) {
		purchaseService.delete(jwt.userId(), tripId, id)
	}
}
