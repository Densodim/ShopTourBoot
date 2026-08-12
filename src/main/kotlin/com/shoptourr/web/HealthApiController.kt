package com.shoptourr.web

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
class HealthApiController {

	@GetMapping("/_ping")
	fun ping(): Map<String, String> = mapOf("status" to "ok")
}
