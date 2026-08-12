package com.shoptourr

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
class VoyageApplication

fun main(args: Array<String>) {
	runApplication<VoyageApplication>(*args)
}
