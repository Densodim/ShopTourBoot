package com.shoptourr.analytics.dto

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Size

data class AnalyticsBatchRequest(
	@field:NotEmpty
	@field:Size(max = 100)
	@field:Valid
	val events: List<AnalyticsEventIngestDto>,

	@field:Size(max = 64)
	val userId: String? = null,
)

data class AnalyticsEventIngestDto(
	@field:NotBlank
	@field:Size(max = 64)
	val id: String,

	@field:NotBlank
	@field:Size(max = 120)
	val name: String,

	val properties: Map<String, String> = emptyMap(),

	@field:NotBlank
	val timestamp: String,
)
