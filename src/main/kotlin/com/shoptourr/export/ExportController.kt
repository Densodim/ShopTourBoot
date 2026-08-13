package com.shoptourr.export

import com.shoptourr.export.dto.CreateExportRequest
import com.shoptourr.export.dto.ExportJobDto
import com.shoptourr.identity.userId
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.net.URI
import java.util.UUID

@RestController
class ExportController(
	private val exportService: ExportService,
) {

	@PostMapping("/api/trips/{tripId}/exports")
	fun create(
		@AuthenticationPrincipal jwt: Jwt,
		@PathVariable tripId: UUID,
		@Valid @RequestBody request: CreateExportRequest,
	): ResponseEntity<ExportJobDto> {
		val body = exportService.create(jwt.userId(), tripId, request)
		return ResponseEntity.status(HttpStatus.ACCEPTED)
			.location(URI.create("/api/exports/${body.id}"))
			.body(body)
	}

	@GetMapping("/api/exports/{exportId}")
	fun get(
		@AuthenticationPrincipal jwt: Jwt,
		@PathVariable exportId: UUID,
	) = exportService.get(jwt.userId(), exportId)
}
