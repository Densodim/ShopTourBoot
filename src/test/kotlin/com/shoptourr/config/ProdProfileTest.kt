package com.shoptourr.config

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.boot.env.YamlPropertySourceLoader
import org.springframework.core.io.ClassPathResource

/**
 * The prod profile is what keeps the API surface unpublished in production. A silent typo here
 * would only be noticed by whoever finds the documentation on the public internet.
 */
class ProdProfileTest {

	private val properties = YamlPropertySourceLoader()
		.load("prod", ClassPathResource("application-prod.yml"))
		.first()

	@Test
	fun `openapi document is disabled`() {
		assertEquals(false, properties.getProperty("springdoc.api-docs.enabled"))
	}

	@Test
	fun `swagger ui is disabled`() {
		assertEquals(false, properties.getProperty("springdoc.swagger-ui.enabled"))
	}
}
