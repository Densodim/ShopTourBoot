package com.shoptourr.idempotency

import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import tools.jackson.databind.json.JsonMapper

@Configuration
class IdempotencyConfig {

	@Bean
	fun idempotencyFilter(
		idempotencyService: IdempotencyService,
		jsonMapper: JsonMapper,
	): FilterRegistrationBean<IdempotencyFilter> {
		val registration = FilterRegistrationBean(IdempotencyFilter(idempotencyService, jsonMapper))
		registration.setName("idempotencyFilter")
		registration.addUrlPatterns("/api/*")
		registration.order = Ordered.LOWEST_PRECEDENCE
		return registration
	}
}
