package com.shoptourr.insights

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Fallback geocoder when Nominatim is down or the query is unknown.
 * Named places and trip cities resolve from a static table.
 */
object PlaceCatalog {

	data class LatLng(val lat: BigDecimal, val lng: BigDecimal)

	fun resolve(place: String?, city: String?, country: String?, countryCode: String?): LatLng? =
		lookup(PLACES, place)
			?: lookup(CITIES, city)
			?: countryCode?.uppercase()?.let { COUNTRY[it] }
			?: lookup(COUNTRY_NAME, country)

	internal fun lookup(table: Map<String, LatLng>, raw: String?): LatLng? {
		val key = normalize(raw) ?: return null
		table[key]?.let { return it }
		if (key.length < 4) {
			return null
		}
		return table.entries
			.filter { entry -> key.contains(entry.key) || entry.key.contains(key) }
			.maxByOrNull { it.key.length }
			?.value
	}

	private fun normalize(raw: String?): String? =
		raw?.trim()?.lowercase()?.replace(Regex("\\s+"), " ")?.takeIf { it.isNotBlank() }

	private fun ll(lat: String, lng: String) = LatLng(BigDecimal(lat).setScale(6, RoundingMode.HALF_UP), BigDecimal(lng).setScale(6, RoundingMode.HALF_UP))

	private val CITIES: Map<String, LatLng> = mapOf(
		"lisbon" to ll("38.722300", "-9.139300"),
		"lisboa" to ll("38.722300", "-9.139300"),
		"porto" to ll("41.157900", "-8.629100"),
		"tokyo" to ll("35.676200", "139.650300"),
		"osaka" to ll("34.693700", "135.502300"),
		"paris" to ll("48.856600", "2.352200"),
		"rome" to ll("41.902800", "12.496400"),
		"madrid" to ll("40.416800", "-3.703800"),
		"barcelona" to ll("41.387400", "2.168600"),
		"berlin" to ll("52.520000", "13.405000"),
		"munich" to ll("48.135100", "11.582000"),
		"london" to ll("51.507400", "-0.127800"),
		"oslo" to ll("59.913900", "10.752200"),
		"bergen" to ll("60.391300", "5.322100"),
		"new york" to ll("40.712800", "-74.006000"),
		"prague" to ll("50.075500", "14.437800"),
		"vienna" to ll("48.208200", "16.373800"),
		"amsterdam" to ll("52.367600", "4.904100"),
	)

	private val PLACES: Map<String, LatLng> = mapOf(
		"time out market" to ll("38.706900", "-9.145700"),
		"belem tower" to ll("38.691600", "-9.216000"),
		"jeronimos monastery" to ll("38.697900", "-9.206700"),
		"senso-ji" to ll("35.714800", "139.796700"),
		"sensoji" to ll("35.714800", "139.796700"),
		"shibuya crossing" to ll("35.659500", "139.700500"),
		"eiffel tower" to ll("48.858400", "2.294500"),
		"colosseum" to ll("41.890200", "12.492200"),
		"sagrada familia" to ll("41.403600", "2.174400"),
		"brandenburg gate" to ll("52.516300", "13.377700"),
		"big ben" to ll("51.500700", "-0.124600"),
		"times square" to ll("40.758000", "-73.985500"),
	)

	private val COUNTRY: Map<String, LatLng> = mapOf(
		"PT" to CITIES.getValue("lisbon"),
		"JP" to CITIES.getValue("tokyo"),
		"FR" to CITIES.getValue("paris"),
		"IT" to CITIES.getValue("rome"),
		"ES" to CITIES.getValue("madrid"),
		"DE" to CITIES.getValue("berlin"),
		"GB" to CITIES.getValue("london"),
		"NO" to CITIES.getValue("oslo"),
		"US" to CITIES.getValue("new york"),
	)

	private val COUNTRY_NAME: Map<String, LatLng> = mapOf(
		"portugal" to CITIES.getValue("lisbon"),
		"japan" to CITIES.getValue("tokyo"),
		"france" to CITIES.getValue("paris"),
		"italy" to CITIES.getValue("rome"),
		"spain" to CITIES.getValue("madrid"),
		"germany" to CITIES.getValue("berlin"),
		"united kingdom" to CITIES.getValue("london"),
		"norway" to CITIES.getValue("oslo"),
		"united states" to CITIES.getValue("new york"),
	)
}
