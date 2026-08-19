package com.shoptourr.purchase

import com.shoptourr.DomainValidationException
import java.time.Instant

object PurchaseEtag {
	fun of(updatedAt: Instant): String = "\"$updatedAt\""

	fun parse(header: String?): Instant? {
		if (header.isNullOrBlank()) return null
		val unquoted = header.trim()
			.removePrefix("W/")
			.trim()
			.removePrefix("\"")
			.removeSuffix("\"")
		return runCatching { Instant.parse(unquoted) }.getOrElse {
			throw DomainValidationException("If-Match must be the purchase updatedAt instant.")
		}
	}
}
