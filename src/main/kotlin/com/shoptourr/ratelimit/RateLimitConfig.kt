package com.shoptourr.ratelimit

import com.shoptourr.config.RateLimitProperties
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import tools.jackson.databind.json.JsonMapper
import java.time.Clock

@Configuration
class RateLimitConfig {

	@Bean
	fun rateLimitFilter(
		rateLimits: RateLimitService,
		properties: RateLimitProperties,
		jsonMapper: JsonMapper,
		clock: Clock,
	): FilterRegistrationBean<RateLimitFilter> {
		val registration = FilterRegistrationBean(RateLimitFilter(rateLimits, properties, jsonMapper, clock))
		registration.setName("rateLimitFilter")
		registration.addUrlPatterns("/api/*", "/dev-uploads/*", "/dev-exports/*")
		registration.order = Ordered.HIGHEST_PRECEDENCE + 20
		return registration
	}
}
