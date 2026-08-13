package com.shoptourr.wishlist

import com.shoptourr.identity.userId
import com.shoptourr.wishlist.dto.CreateWishlistItemRequest
import com.shoptourr.wishlist.dto.UpdateWishlistItemRequest
import com.shoptourr.wishlist.dto.WishlistItemDto
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
@RequestMapping("/api/wishlist")
class WishlistController(
	private val wishlistService: WishlistService,
) {

	@GetMapping
	fun list(@AuthenticationPrincipal jwt: Jwt) = wishlistService.list(jwt.userId())

	@PostMapping
	fun create(
		@AuthenticationPrincipal jwt: Jwt,
		@Valid @RequestBody request: CreateWishlistItemRequest,
	): ResponseEntity<WishlistItemDto> {
		val body = wishlistService.create(jwt.userId(), request)
		return ResponseEntity.created(URI.create("/api/wishlist/${body.id}")).body(body)
	}

	@PatchMapping("/{id}")
	fun update(
		@AuthenticationPrincipal jwt: Jwt,
		@PathVariable id: UUID,
		@Valid @RequestBody request: UpdateWishlistItemRequest,
	) = wishlistService.update(jwt.userId(), id, request)

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	fun delete(
		@AuthenticationPrincipal jwt: Jwt,
		@PathVariable id: UUID,
	) {
		wishlistService.delete(jwt.userId(), id)
	}
}
