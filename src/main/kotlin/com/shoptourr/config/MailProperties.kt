package com.shoptourr.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "voyage.mail")
data class MailProperties(
	val from: String = "noreply@shoptourr.com",
)
