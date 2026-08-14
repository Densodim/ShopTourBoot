package com.shoptourr.geo

import com.shoptourr.config.GeoProperties
import com.shoptourr.insights.PlaceCatalog
import org.slf4j.LoggerFactory
import org.springframework.data.redis.RedisConnectionFailureException
import org.springframework.data.redis.RedisSystemException
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service

@Service
class GeocodeService(
	private val live: LiveGeocodeClient,
	private val redis: StringRedisTemplate,
	private val properties: GeoProperties,
) {

	private val log = LoggerFactory.getLogger(javaClass)

	fun resolve(place: String?, city: String?, country: String?, countryCode: String?): PlaceCatalog.LatLng? {
		val query = listOf(place, city, country).mapNotNull { it?.trim()?.takeIf(String::isNotBlank) }.joinToString(", ")
		if (query.isNotBlank() && properties.enabled) {
			cached(query)?.let { return it }
			live.search(query)?.let { point ->
				writeCache(query, point)
				return point
			}
		}
		return PlaceCatalog.resolve(place, city, country, countryCode)
	}

	private fun cached(query: String): PlaceCatalog.LatLng? {
		return try {
			redis.opsForValue().get(cacheKey(query))?.let { decode(it) }
		} catch (ex: RedisConnectionFailureException) {
			log.warn("Geocode cache unavailable", ex)
			null
		} catch (ex: RedisSystemException) {
			log.warn("Geocode cache unavailable", ex)
			null
		}
	}

	private fun writeCache(query: String, point: PlaceCatalog.LatLng) {
		try {
			redis.opsForValue().set(cacheKey(query), encode(point), properties.cacheTtl)
		} catch (ex: RedisConnectionFailureException) {
			log.warn("Geocode cache write failed", ex)
		} catch (ex: RedisSystemException) {
			log.warn("Geocode cache write failed", ex)
		}
	}

	companion object {
		fun cacheKey(query: String): String = "geo:live:${query.trim().lowercase()}"

		internal fun encode(point: PlaceCatalog.LatLng): String =
			"${point.lat.toPlainString()},${point.lng.toPlainString()}"

		internal fun decode(raw: String): PlaceCatalog.LatLng? {
			val sep = raw.indexOf(',')
			if (sep <= 0) {
				return null
			}
			val lat = raw.substring(0, sep).toBigDecimalOrNull() ?: return null
			val lng = raw.substring(sep + 1).toBigDecimalOrNull() ?: return null
			return PlaceCatalog.LatLng(lat, lng)
		}
	}
}
