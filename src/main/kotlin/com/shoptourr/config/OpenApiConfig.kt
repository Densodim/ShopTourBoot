package com.shoptourr.config

import com.shoptourr.web.ApiProblem
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.PathItem
import io.swagger.v3.oas.models.media.ArraySchema
import io.swagger.v3.oas.models.media.Content
import io.swagger.v3.oas.models.media.IntegerSchema
import io.swagger.v3.oas.models.media.MediaType
import io.swagger.v3.oas.models.media.ObjectSchema
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.media.StringSchema
import io.swagger.v3.oas.models.responses.ApiResponse
import org.springdoc.core.customizers.OpenApiCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE
import org.springframework.util.AntPathMatcher

/**
 * Publishes the error contract in the OpenAPI document.
 *
 * Every controller returns the same `ProblemDetail` shape on failure, so the responses are
 * attached globally instead of annotating each handler — a per-endpoint `@ApiResponse` still
 * wins, because an explicitly declared status is never overwritten here.
 */
@Configuration
class OpenApiConfig {

	companion object {
		const val PROBLEM_SCHEMA = "ProblemDetail"
		private const val SCHEMA_REF = "#/components/schemas/$PROBLEM_SCHEMA"
	}

	private val pathMatcher = AntPathMatcher()

	@Bean
	fun problemDetailResponsesCustomizer(): OpenApiCustomizer = OpenApiCustomizer { openApi ->
		registerSchema(openApi)

		openApi.paths?.forEach { (path, pathItem) ->
			val public = isPublic(path)
			pathItem.readOperationsMap()?.forEach { (method, operation) ->
				statusesFor(method, path, public).forEach { (status, description) ->
					addIfAbsent(operation, status, description)
				}
			}
		}
	}

	private fun isPublic(path: String): Boolean =
		PublicEndpoints.API.any { pathMatcher.match(it, path) }

	/**
	 * Documenting a 401 on a public endpoint, or a 409 on a read, would be a lie — the set is
	 * narrowed to what an operation can actually produce.
	 */
	private fun statusesFor(
		method: PathItem.HttpMethod,
		path: String,
		public: Boolean,
	): List<Pair<String, String>> = buildList {
		if (!public) {
			add("401" to "Missing, expired or invalid token (`code`: ${ApiProblem.UNAUTHORIZED}).")
			add("403" to "Authenticated, but not allowed to access this resource (`code`: ${ApiProblem.FORBIDDEN}).")
		}
		if (method != PathItem.HttpMethod.GET && method != PathItem.HttpMethod.HEAD) {
			add("400" to "Malformed body or failed validation (`code`: ${ApiProblem.VALIDATION_ERROR}); `errors` lists the offending fields.")
			add("409" to "Conflicts with the current state of the resource (`code`: ${ApiProblem.CONFLICT}).")
		}
		if (path.contains('{')) {
			add("404" to "No such resource (`code`: ${ApiProblem.NOT_FOUND}).")
		}
		add("500" to "Unexpected failure (`code`: ${ApiProblem.INTERNAL_ERROR}); quote the `X-Request-Id` response header when reporting it.")
	}

	private fun addIfAbsent(operation: Operation, status: String, description: String) {
		val responses = operation.responses ?: return
		if (responses.containsKey(status)) {
			return
		}
		responses.addApiResponse(
			status,
			ApiResponse()
				.description(description)
				.content(
					Content().addMediaType(
						APPLICATION_PROBLEM_JSON_VALUE,
						MediaType().schema(Schema<Any>().`$ref`(SCHEMA_REF)),
					),
				),
		)
	}

	private fun registerSchema(openApi: OpenAPI) {
		val components = openApi.components ?: Components().also { openApi.components = it }
		components.addSchemas(PROBLEM_SCHEMA, problemDetailSchema())
	}

	private fun problemDetailSchema(): Schema<*> =
		ObjectSchema()
			.description("RFC 9457 problem detail. `code` is the stable, machine-readable discriminator — branch on it, not on `detail`.")
			.addProperty(
				"type",
				StringSchema().format("uri")
					.example("https://api.shoptourr.com/problems/validation-error"),
			)
			.addProperty("title", StringSchema().example("Validation failed"))
			.addProperty("status", IntegerSchema().format("int32").example(400))
			.addProperty("detail", StringSchema().description("Human-readable explanation. Not stable — never parse it."))
			.addProperty("instance", StringSchema().format("uri").example("/api/orders"))
			.addProperty(
				"code",
				StringSchema()
					.description("Stable error code, `SCREAMING_SNAKE_CASE`.")
					.example(ApiProblem.VALIDATION_ERROR),
			)
			.addProperty(
				"errors",
				ArraySchema()
					.description("Field-level failures. Present on ${ApiProblem.VALIDATION_ERROR} only.")
					.items(
						ObjectSchema()
							.addProperty("field", StringSchema().example("email"))
							.addProperty("code", StringSchema().example("Email"))
							.addProperty("message", StringSchema().example("must be a well-formed email address")),
					),
			)
}
