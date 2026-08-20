package com.shoptourr.media

import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

/**
 * Public PUT/GET for media bytes, plus tus-lite HEAD/PATCH resume.
 * The KMP client uploads to [MediaUploadIntentResponse.uploadUrl] without a Bearer token.
 */
@RestController
@RequestMapping("/dev-uploads")
class DevUploadController(
	private val mediaService: MediaService,
) {

	@PutMapping("/{mediaId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	fun put(
		@PathVariable mediaId: UUID,
		@RequestBody body: ByteArray,
	) {
		mediaService.storeBytes(mediaId, body)
	}

	@RequestMapping(path = ["/{mediaId}"], method = [RequestMethod.HEAD])
	fun head(@PathVariable mediaId: UUID): ResponseEntity<Void> {
		val offset = mediaService.uploadOffset(mediaId)
		return tusOffset(offset)
	}

	@PatchMapping("/{mediaId}")
	fun patch(
		@PathVariable mediaId: UUID,
		@RequestHeader("Upload-Offset") offset: Long,
		@RequestBody body: ByteArray,
	): ResponseEntity<Void> {
		val next = mediaService.appendBytes(mediaId, offset, body)
		return tusOffset(next)
	}

	@GetMapping("/{mediaId}")
	fun get(@PathVariable mediaId: UUID): ResponseEntity<ByteArray> {
		val stored = mediaService.loadBytes(mediaId)
		return ResponseEntity.ok()
			.contentType(MediaType.parseMediaType(stored.contentType))
			.body(stored.bytes)
	}

	private fun tusOffset(offset: Long): ResponseEntity<Void> =
		ResponseEntity.noContent()
			.header("Tus-Resumable", "1.0.0")
			.header("Upload-Offset", offset.toString())
			.header("Cache-Control", "no-store")
			.build()
}
