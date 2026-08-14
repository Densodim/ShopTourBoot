package com.shoptourr.export

import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

/**
 * Local stand-in for a pre-signed object-storage GET.
 * The KMP client opens [ExportJobDto.downloadUrl] without a Bearer token.
 */
@RestController
@RequestMapping("/dev-exports")
class DevExportController(
	private val exportService: ExportService,
) {

	@GetMapping("/{exportId}")
	fun get(@PathVariable exportId: UUID): ResponseEntity<ByteArray> {
		val stored = exportService.loadFile(exportId)
		return ResponseEntity.ok()
			.contentType(MediaType.parseMediaType(stored.contentType))
			.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"${stored.filename}\"")
			.body(stored.bytes)
	}
}
