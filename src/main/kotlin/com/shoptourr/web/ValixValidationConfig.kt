package com.shoptourr.web

import io.valix.generated.ValixRegistry
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

/**
 * Replaces `valix-spring`'s own MVC wiring, which does not validate anything as shipped in 1.0.3.
 *
 * KSP generates `ValixRegistry.validate(value, vararg groups, failFast = false)`. Because
 * `failFast` follows the vararg it is a real third JVM parameter, so the generated method is
 * `validate(Object, KClass[], boolean)` — but the library's `ValixFrameworkValidator` looks it up
 * reflectively as `getMethod("validate", Any::class.java, Array::class.java)`. The resulting
 * `NoSuchMethodException` is swallowed by a blanket `catch`, installing a validator that reports
 * every payload as valid, so `@ValidValix` silently validates nothing.
 *
 * This configuration calls the generated registry the way the Valix docs describe instead — a
 * plain import, since KSP output is compiled into this module — so there is no signature lookup
 * to drift and no fallback to pass everything. `ValixSpringAutoConfiguration` is excluded on
 * [com.shoptourr.VoyageApplication]; the `@ValidValix` annotation and [ValixValidationException]
 * still come from the library.
 *
 * `ValixRegistry.validate` returns "valid" for a class it has no entry for, so a DTO KSP did not
 * pick up would pass unchecked. `ValixRegistryCoverageTest` fails the build in that case.
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

		val result = ValixRegistry.validate(argument, *annotation.groups)
		if (!result.valid) {
			throw ValixValidationException(result)
		}
		return argument
	}
}
