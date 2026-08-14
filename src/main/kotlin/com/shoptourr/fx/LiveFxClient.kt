package com.shoptourr.fx

import com.shoptourr.config.FxProperties
import org.slf4j.LoggerFactory
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import java.math.BigDecimal
import java.net.http.HttpClient

@Component
class LiveFxClient(
	properties: FxProperties,
) {

	private val log = LoggerFactory.getLogger(javaClass)
	private val enabled = properties.enabled
	private val restClient: RestClient = RestClient.builder()
		.baseUrl(properties.baseUrl.trimEnd('/'))
		.requestFactory(
			JdkClientHttpRequestFactory(
				HttpClient.newBuilder().connectTimeout(properties.connectTimeout).build(),
			).also { it.setReadTimeout(properties.readTimeout) },
		)
		.build()

	fun rates(base: String): Map<String, BigDecimal>? {
		if (!enabled) {
			return null
		}
		return try {
			val body = restClient.get()
				.uri("/{base}", base.uppercase())
				.retrieve()
				.body(ErApiLatestResponse::class.java)
			if (body?.result != "success") {
				null
			} else {
				body.rates?.filterValues { it.signum() > 0 }
			}
		} catch (ex: RestClientException) {
			log.warn("Live FX lookup failed for base={}", base, ex)
			null
		}
	}

	data class ErApiLatestResponse(
		val result: String? = null,
		val rates: Map<String, BigDecimal>? = null,
	)
}
