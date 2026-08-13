package com.shoptourr.insights

import com.shoptourr.identity.userId
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api")
class InsightsController(
	private val insightsService: InsightsService,
) {

	@GetMapping("/home")
	fun home(@AuthenticationPrincipal jwt: Jwt) = insightsService.home(jwt.userId())

	@GetMapping("/trips/{tripId}/stats")
	fun stats(@AuthenticationPrincipal jwt: Jwt, @PathVariable tripId: UUID) =
		insightsService.stats(jwt.userId(), tripId)

	@GetMapping("/trips/{tripId}/alerts")
	fun alerts(@AuthenticationPrincipal jwt: Jwt, @PathVariable tripId: UUID) =
		insightsService.alerts(jwt.userId(), tripId)

	@GetMapping("/trips/{tripId}/tax-free")
	fun taxFree(@AuthenticationPrincipal jwt: Jwt, @PathVariable tripId: UUID) =
		insightsService.taxFree(jwt.userId(), tripId)

	@GetMapping("/trips/{tripId}/route")
	fun route(@AuthenticationPrincipal jwt: Jwt, @PathVariable tripId: UUID) =
		insightsService.route(jwt.userId(), tripId)
}
