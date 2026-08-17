package com.shoptourr.web

import io.valix.generated.ValixRegistry
import io.valix.spring.ValidValix
import org.junit.jupiter.api.Test
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider
import org.springframework.core.type.filter.AnnotationTypeFilter
import org.springframework.web.bind.annotation.RestController
import kotlin.reflect.KClass
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Closes Valix's remaining fail-open path.
 *
 * `ValixRegistry.validate` returns a valid result for any class it has no entry for, so a
 * `@ValidValix` body whose DTO KSP never picked up would be accepted unchecked, with nothing
 * logged. Rather than paying for a reflective guard on every request, this test scans the
 * controllers and asserts every `@ValidValix` parameter type is actually in the registry — a
 * build failure instead of a silent hole in production.
 *
 * Reading the registry's private `validators` map is deliberate white-box access, kept out of
 * production code: [ValixValidationConfig] imports the generated registry directly and does no
 * reflection at all.
 */
class ValixRegistryCoverageTest {

	@Test
	fun `every ValidValix request body has a generated validator`() {
		val annotated = validValixParameterTypes()

		if (annotated.isEmpty()) {
			fail("No @ValidValix parameters found — the scan is broken, not the wiring.")
		}

		val missing = annotated - registeredTypes()
		assertTrue(
			missing.isEmpty(),
			"These @ValidValix types are absent from ValixRegistry and would be accepted " +
				"unvalidated: ${missing.map { it.qualifiedName }}",
		)
	}

	private fun validValixParameterTypes(): Set<KClass<*>> {
		val scanner = ClassPathScanningCandidateComponentProvider(false).apply {
			addIncludeFilter(AnnotationTypeFilter(RestController::class.java))
		}
		return scanner.findCandidateComponents(BASE_PACKAGE)
			.mapNotNull { it.beanClassName }
			.map { Class.forName(it) }
			.flatMap { controller -> controller.declaredMethods.toList() }
			.flatMap { method ->
				method.parameters.filter { it.isAnnotationPresent(ValidValix::class.java) }
			}
			.map { it.type.kotlin }
			.toSet()
	}

	private fun registeredTypes(): Set<KClass<*>> {
		@Suppress("UNCHECKED_CAST")
		val validators = ValixRegistry::class.java
			.getDeclaredField("validators")
			.apply { isAccessible = true }
			.get(ValixRegistry) as Map<KClass<*>, *>
		return validators.keys
	}

	private companion object {
		const val BASE_PACKAGE = "com.shoptourr"
	}
}
