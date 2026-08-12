package com.shoptourr.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.PathItem
import io.swagger.v3.oas.models.Paths
import io.swagger.v3.oas.models.responses.ApiResponse
import io.swagger.v3.oas.models.responses.ApiResponses
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE

class OpenApiConfigTest {

	private val customizer = OpenApiConfig().problemDetailResponsesCustomizer()

	private fun operation() = Operation().responses(
		ApiResponses().addApiResponse("200", ApiResponse().description("OK")),
	)

	private fun openApi(): OpenAPI {
		val paths = Paths()
		paths.addPathItem("/api/_ping", PathItem().get(operation()))
		paths.addPathItem(
			"/api/orders/{id}",
			PathItem().get(operation()).post(operation()),
		)
		paths.addPathItem("/api/orders", PathItem().get(operation()))
		return OpenAPI().paths(paths)
	}

	private fun customized(): OpenAPI = openApi().also { customizer.customise(it) }

	private fun responses(api: OpenAPI, path: String, read: (PathItem) -> Operation?) =
		read(api.paths[path]!!)!!.responses.keys

	@Test
	fun `the problem detail schema is registered once, with the code discriminator`() {
		val api = customized()

		val schema = api.components.schemas[OpenApiConfig.PROBLEM_SCHEMA]
		assertNotNull(schema)
		assertTrue(schema!!.properties.containsKey("code"), schema.properties.keys.toString())
		assertTrue(schema.properties.containsKey("errors"), schema.properties.keys.toString())
	}

	@Test
	fun `public endpoints are not documented as returning 401 or 403`() {
		val statuses = responses(customized(), "/api/_ping") { it.get }

		assertFalse(statuses.contains("401"), statuses.toString())
		assertFalse(statuses.contains("403"), statuses.toString())
		assertTrue(statuses.contains("500"), statuses.toString())
	}

	@Test
	fun `protected endpoints document the auth failures`() {
		val statuses = responses(customized(), "/api/orders") { it.get }

		assertTrue(statuses.containsAll(listOf("401", "403", "500")), statuses.toString())
	}

	@Test
	fun `reads do not claim they can return 400 or 409`() {
		val statuses = responses(customized(), "/api/orders") { it.get }

		assertFalse(statuses.contains("400"), statuses.toString())
		assertFalse(statuses.contains("409"), statuses.toString())
	}

	@Test
	fun `writes document validation and conflict failures`() {
		val statuses = responses(customized(), "/api/orders/{id}") { it.post }

		assertTrue(statuses.containsAll(listOf("400", "409")), statuses.toString())
	}

	@Test
	fun `only endpoints addressing a resource by id document 404`() {
		val withId = responses(customized(), "/api/orders/{id}") { it.get }
		val collection = responses(customized(), "/api/orders") { it.get }

		assertTrue(withId.contains("404"), withId.toString())
		assertFalse(collection.contains("404"), collection.toString())
	}

	@Test
	fun `error responses point at the shared schema as problem json`() {
		val response = customized().paths["/api/orders"]!!.get.responses["401"]!!
		val mediaType = response.content[APPLICATION_PROBLEM_JSON_VALUE]

		assertNotNull(mediaType)
		assertEquals("#/components/schemas/${OpenApiConfig.PROBLEM_SCHEMA}", mediaType!!.schema.`$ref`)
	}

	@Test
	fun `an explicitly declared response is never overwritten`() {
		val api = openApi()
		api.paths["/api/orders"]!!.get.responses.addApiResponse(
			"401",
			ApiResponse().description("Hand-written, keep me"),
		)

		customizer.customise(api)

		assertEquals(
			"Hand-written, keep me",
			api.paths["/api/orders"]!!.get.responses["401"]!!.description,
		)
	}

	@Test
	fun `the success response is left alone`() {
		val response = customized().paths["/api/_ping"]!!.get.responses["200"]!!

		assertEquals("OK", response.description)
	}
}
