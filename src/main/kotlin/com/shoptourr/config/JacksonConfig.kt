package com.shoptourr.config

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.module.SimpleModule
import tools.jackson.databind.ser.std.ToStringSerializer
import java.math.BigDecimal

/**
 * Money amounts as JSON strings ("96.50") per API contract.
 * Jackson 3 (Boot 4).
 *
 * This customizes Boot's auto-configured mapper instead of replacing it with an own
 * `JsonMapper` bean: a replacement backs off `JacksonAutoConfiguration`, and with it the
 * `ProblemDetail` mixin that flattens the `properties` map into top-level JSON fields —
 * which silently breaks the RFC 9457 error contract. Pinned by `JacksonConfigTest`.
 */
@Configuration
class JacksonConfig {

	@Bean
	fun apiJsonMapperCustomizer(): JsonMapperBuilderCustomizer =
		JsonMapperBuilderCustomizer { builder ->
			builder
				.addModule(
					SimpleModule().addSerializer(BigDecimal::class.java, ToStringSerializer.instance),
				)
				.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
		}
}
