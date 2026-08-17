package com.shoptourr.web

import io.valix.core.ValidationResult
import io.valix.spring.ValidValix
import io.valix.spring.ValixValidationException
import org.springframework.beans.factory.config.BeanPostProcessor
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.MethodParameter
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter
import kotlin.reflect.KClass

/**
 * Replaces `valix-spring`'s own MVC wiring, which is inert in 1.0.3.
 *
 * KSP generates `ValixRegistry.validate(value, vararg groups, failFast = false)`. Because
 * `failFast` follows the vararg it is a real third JVM parameter, so the generated method is
 * `validate(Object, KClass[], boolean)` — but the library's `ValixFrameworkValidator` looks it up
 * as `getMethod("validate", Any::class.java, Array::class.java)`. The resulting
 * `NoSuchMethodException` is swallowed by a blanket `catch`, which installs a validator that
 * reports every payload as valid. `@ValidValix` then silently validates nothing.
 *
 * `ValixSpringAutoConfiguration` is excluded on [com.shoptourr.VoyageApplication] and this
 * configuration takes its place: same `@ValidValix` annotation and same
 * [ValixValidationException], but the registry is resolved once, eagerly, and a failure to
 * resolve it is fatal rather than silent.
 */
@Configuration(proxyBeanMethods = false)
class ValixValidationConfig {

	@Bean
	fun valixArgumentResolverPostProcessor(): BeanPostProcessor = object : BeanPostProcessor {
		override fun postProcessAfterInitialization(bean: Any, beanName: String): Any {
			if (bean is RequestMappingHandlerAdapter) {
				bean.argumentResolvers = bean.argumentResolvers?.map { resolver ->
					if (resolver is ValixArgumentResolver) resolver else ValixArgumentResolver(resolver)
				}
			}
			return bean
		}
	}
}

/**
 * Validates `@ValidValix` `@RequestBody` arguments after [delegate] has deserialised them,
 * throwing [ValixValidationException] so [ApiExceptionHandler] can render the project's
 * `VALIDATION_ERROR` problem detail.
 */
class ValixArgumentResolver(
	private val delegate: HandlerMethodArgumentResolver,
) : HandlerMethodArgumentResolver {

	/**
	 * Transparent: the wrapper must support exactly what [delegate] supports. Narrowing this to
	 * `@ValidValix` parameters — as `valix-spring`'s own resolver does — leaves every other
	 * parameter with no resolver at all, and any handler taking one fails with a 500.
	 */
	override fun supportsParameter(parameter: MethodParameter): Boolean =
		delegate.supportsParameter(parameter)

	override fun resolveArgument(
		parameter: MethodParameter,
		mavContainer: ModelAndViewContainer?,
		webRequest: NativeWebRequest,
		binderFactory: WebDataBinderFactory?,
	): Any? {
		val argument = delegate.resolveArgument(parameter, mavContainer, webRequest, binderFactory) ?: return null
		val annotation = parameter.getParameterAnnotation(ValidValix::class.java) ?: return argument

		val result = ValixRegistry.validate(argument, annotation.groups)
		if (!result.valid) {
			throw ValixValidationException(result)
		}
		return argument
	}
}

/**
 * The KSP-generated `io.valix.generated.ValixRegistry`, reached by reflection because the
 * generated class is not on the compile classpath of the code that calls it.
 *
 * Resolution happens once in the initialiser and throws on failure, so a codegen pipeline that
 * stopped producing a registry breaks the application context instead of turning validation off.
 * [validate] still rejects an unregistered class for the same reason: the registry's own
 * `validate` returns "valid" for a type it has no entry for.
 */
object ValixRegistry {

	private val instance: Any
	private val validateMethod: java.lang.reflect.Method
	private val registeredTypes: Set<KClass<*>>

	init {
		val registryClass = runCatching { Class.forName("io.valix.generated.ValixRegistry") }
			.getOrElse {
				throw IllegalStateException(
					"io.valix.generated.ValixRegistry is missing — the Valix KSP processor did not run. " +
						"Validation would silently pass every request body; refusing to start.",
					it,
				)
			}
		instance = registryClass.getField("INSTANCE").get(null)
		val kClassArrayType = java.lang.reflect.Array.newInstance(KClass::class.java, 0).javaClass
		validateMethod = runCatching {
			registryClass.getMethod("validate", Any::class.java, kClassArrayType, Boolean::class.java)
		}.getOrElse {
			throw IllegalStateException(
				"ValixRegistry.validate does not have the expected (Any, KClass[], Boolean) signature. " +
					"Valix changed its generated API; validation cannot be trusted. Refusing to start.",
				it,
			)
		}
		@Suppress("UNCHECKED_CAST")
		val validators = registryClass.getDeclaredField("validators")
			.apply { isAccessible = true }
			.get(instance) as Map<KClass<*>, *>
		registeredTypes = validators.keys
	}

	/** Types KSP generated a validator for. Used by tests to assert a DTO was actually picked up. */
	fun registeredTypes(): Set<KClass<*>> = registeredTypes

	fun validate(value: Any, groups: Array<KClass<*>> = emptyArray()): ValidationResult {
		check(value::class in registeredTypes) {
			"${value::class.qualifiedName} is annotated for Valix validation but is absent from " +
				"ValixRegistry, which would pass it unchecked."
		}
		return validateMethod.invoke(instance, value, groups, false) as ValidationResult
	}
}
