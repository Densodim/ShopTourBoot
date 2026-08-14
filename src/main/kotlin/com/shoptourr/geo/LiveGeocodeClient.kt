package com.shoptourr.geo

import com.shoptourr.config.GeoProperties
import com.shoptourr.insights.PlaceCatalog
import org.slf4j.LoggerFactory
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import java.math.BigDecimal
import java.math.RoundingMode
import java.net.http.HttpClient

@Component
class LiveGeocodeClient(
	properties: GeoProperties,
) {

	private val log = LoggerFactory.getLogger(javaClass)
	private val enabled = properties.enabled
	private val restClient: RestClient = RestClient.builder()
		.baseUrl(properties.baseUrl.trimEnd('/'))
		.defaultHeader("User-Agent", properties.userAgent)
		.defaultHeader("Accept", "application/json")
		.requestFactory(
			JdkClientHttpRequestFactory(
				HttpClient.newBuilder().connectTimeout(properties.connectTimeout).build(),
			).also { it.setReadTimeout(properties.readTimeout) },
		)
		.build()

	fun search(query: String): PlaceCatalog.LatLng? {
		if (!enabled || query.isBlank()) {
			return null
		}
		return try {
			val hits = restClient.get()
				.uri("/search?q={q}&format=json&limit=1", query)
				.retrieve()
				.body(HITS)
				.orEmpty()
			val hit = hits.firstOrNull() ?: return null
			val lat = hit.lat?.toBigDecimalOrNull() ?: return null
			val lng = hit.lon?.toBigDecimalOrNull() ?: return null
			PlaceCatalog.LatLng(lat.setScale(6, RoundingMode.HALF_UP), lng.setScale(6, RoundingMode.HALF_UP))
		} catch (ex: RestClientException) {
			log.warn("Live geocode failed for query={}", query, ex)
			null
		}
	}

	data class NominatimHit(
		val lat: String? = null,
		val lon: String? = null,
	)

	companion object {
		private val HITS = object : ParameterizedTypeReference<List<NominatimHit>>() {}
	}
}
