package com.shoptourr.analytics

import com.shoptourr.analytics.dto.AnalyticsBatchRequest
import com.shoptourr.identity.userId
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/me/analytics-events")
class AnalyticsController(
	private val analyticsService: AnalyticsService,
) {

	@PostMapping
	@ResponseStatus(HttpStatus.NO_CONTENT)
	fun ingest(
		@AuthenticationPrincipal jwt: Jwt,
		@Valid @RequestBody request: AnalyticsBatchRequest,
	) {
		analyticsService.ingest(jwt.userId(), request)
	}
}
