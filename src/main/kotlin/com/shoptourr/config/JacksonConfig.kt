package com.shoptourr.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.json.JsonMapper
import tools.jackson.databind.module.SimpleModule
import tools.jackson.databind.ser.std.ToStringSerializer
import tools.jackson.module.kotlin.kotlinModule
import java.math.BigDecimal

/**
 * Money amounts as JSON strings ("96.50") per API contract.
 * Jackson 3 (Boot 4).
 */
@Configuration
class JacksonConfig {

	@Bean
	fun jsonMapper(): JsonMapper {
		val money = SimpleModule().addSerializer(BigDecimal::class.java, ToStringSerializer.instance)
		return JsonMapper.builder()
			.addModule(kotlinModule())
			.addModule(money)
			.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
			.build()
	}
}
