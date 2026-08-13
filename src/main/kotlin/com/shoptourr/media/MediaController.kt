package com.shoptourr.media

import com.shoptourr.identity.userId
import com.shoptourr.media.dto.ConfirmMediaUploadRequest
import com.shoptourr.media.dto.CreateMediaUploadIntentRequest
import com.shoptourr.media.dto.MediaAssetDto
import com.shoptourr.media.dto.MediaUploadIntentResponse
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.net.URI
import java.util.UUID

@RestController
@RequestMapping("/api/media")
class MediaController(
	private val mediaService: MediaService,
) {

	@PostMapping("/upload-intents")
	fun createIntent(
		@AuthenticationPrincipal jwt: Jwt,
		@Valid @RequestBody request: CreateMediaUploadIntentRequest,
	): ResponseEntity<MediaUploadIntentResponse> {
		val body = mediaService.createIntent(jwt.userId(), request)
		return ResponseEntity.created(URI.create("/api/media/${body.mediaId}")).body(body)
	}

	@PostMapping("/{mediaId}/confirm")
	fun confirm(
		@AuthenticationPrincipal jwt: Jwt,
		@PathVariable mediaId: UUID,
		@Valid @RequestBody request: ConfirmMediaUploadRequest,
	) = mediaService.confirm(jwt.userId(), mediaId, request)

	@GetMapping("/{mediaId}")
	fun get(
		@AuthenticationPrincipal jwt: Jwt,
		@PathVariable mediaId: UUID,
	) = mediaService.get(jwt.userId(), mediaId)

	@GetMapping("/{mediaId}/ocr")
	fun ocr(
		@AuthenticationPrincipal jwt: Jwt,
		@PathVariable mediaId: UUID,
	) = mediaService.ocr(jwt.userId(), mediaId)
}
