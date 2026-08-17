package com.shoptourr

import io.valix.spring.ValixSpringAutoConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

/**
 * `valix-spring`'s auto-configuration is excluded deliberately: its argument resolver never
 * validates anything in 1.0.3, and its controller advice renders an error shape that is not this
 * API's contract. [com.shoptourr.web.ValixValidationConfig] replaces both.
 */
@SpringBootApplication(exclude = [ValixSpringAutoConfiguration::class])
@ConfigurationPropertiesScan
class VoyageApplication

fun main(args: Array<String>) {
	runApplication<VoyageApplication>(*args)
}
