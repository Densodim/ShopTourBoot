package com.shoptourr.media

import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

/**
 * Public PUT/GET for media bytes. The KMP client uploads to [MediaUploadIntentResponse.uploadUrl]
 * without a Bearer token. The service stores the body in object storage (local disk or S3),
 * not in Postgres.
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

	@GetMapping("/{mediaId}")
	fun get(@PathVariable mediaId: UUID): ResponseEntity<ByteArray> {
		val stored = mediaService.loadBytes(mediaId)
		return ResponseEntity.ok()
			.contentType(MediaType.parseMediaType(stored.contentType))
			.body(stored.bytes)
	}
}
