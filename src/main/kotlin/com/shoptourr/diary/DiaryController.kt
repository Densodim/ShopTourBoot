package com.shoptourr.diary

import com.shoptourr.diary.dto.CreateDiaryEntryRequest
import com.shoptourr.diary.dto.DiaryEntryDto
import com.shoptourr.diary.dto.UpdateDiaryEntryRequest
import com.shoptourr.identity.userId
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
@RequestMapping("/api/trips/{tripId}/diary")
class DiaryController(
	private val diaryService: DiaryService,
) {

	@GetMapping
	fun list(@AuthenticationPrincipal jwt: Jwt, @PathVariable tripId: UUID) =
		diaryService.list(jwt.userId(), tripId)

	@PostMapping
	fun create(
		@AuthenticationPrincipal jwt: Jwt,
		@PathVariable tripId: UUID,
		@Valid @RequestBody request: CreateDiaryEntryRequest,
	): ResponseEntity<DiaryEntryDto> {
		val body = diaryService.create(jwt.userId(), tripId, request)
		return ResponseEntity.created(URI.create("/api/trips/$tripId/diary/${body.id}")).body(body)
	}

	@PatchMapping("/{id}")
	fun update(
		@AuthenticationPrincipal jwt: Jwt,
		@PathVariable tripId: UUID,
		@PathVariable id: UUID,
		@Valid @RequestBody request: UpdateDiaryEntryRequest,
	) = diaryService.update(jwt.userId(), tripId, id, request)

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	fun delete(
		@AuthenticationPrincipal jwt: Jwt,
		@PathVariable tripId: UUID,
		@PathVariable id: UUID,
	) {
		diaryService.delete(jwt.userId(), tripId, id)
	}
}
