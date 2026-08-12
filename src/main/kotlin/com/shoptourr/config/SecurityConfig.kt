package com.shoptourr.config

import com.shoptourr.web.ProblemAccessDeniedHandler
import com.shoptourr.web.ProblemAuthenticationEntryPoint
import org.springframework.boot.security.autoconfigure.actuate.web.servlet.EndpointRequest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
class SecurityConfig(
	private val corsProperties: CorsProperties,
) {

	@Bean
	fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

	@Bean
	fun corsConfigurationSource(): CorsConfigurationSource {
		val configuration = CorsConfiguration().apply {
			allowedOrigins = corsProperties.allowedOrigins
			allowedMethods = corsProperties.allowedMethods
			allowedHeaders = corsProperties.allowedHeaders
			exposedHeaders = corsProperties.exposedHeaders
			allowCredentials = corsProperties.allowCredentials
			maxAge = corsProperties.maxAge.toSeconds()
		}
		return UrlBasedCorsConfigurationSource().apply {
			registerCorsConfiguration("/**", configuration)
		}
	}

	@Bean
	fun securityFilterChain(
		http: HttpSecurity,
		authenticationEntryPoint: ProblemAuthenticationEntryPoint,
		accessDeniedHandler: ProblemAccessDeniedHandler,
	): SecurityFilterChain {
		http
			.csrf { it.disable() }
			.cors(Customizer.withDefaults())
			.sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
			.authorizeHttpRequests { auth ->
				auth
					.requestMatchers(EndpointRequest.to("health", "info")).permitAll()
					.requestMatchers(*PublicEndpoints.API.toTypedArray()).permitAll()
					.requestMatchers(*PublicEndpoints.DOCS.toTypedArray()).permitAll()
					.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
					.anyRequest().authenticated()
			}
			.oauth2ResourceServer { oauth2 ->
				oauth2.jwt(Customizer.withDefaults())
			}
			.exceptionHandling { exceptions ->
				exceptions
					.authenticationEntryPoint(authenticationEntryPoint)
					.accessDeniedHandler(accessDeniedHandler)
			}
		return http.build()
	}
}
