package com.shoptourr.insights

import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

internal object GeoMath {

	fun meters(from: PlaceCatalog.LatLng, to: PlaceCatalog.LatLng): BigDecimal {
		val dLat = Math.toRadians((to.lat - from.lat).toDouble())
		val dLng = Math.toRadians((to.lng - from.lng).toDouble())
		val lat1 = Math.toRadians(from.lat.toDouble())
		val lat2 = Math.toRadians(to.lat.toDouble())
		val h = sin(dLat / 2) * sin(dLat / 2) +
			cos(lat1) * cos(lat2) * sin(dLng / 2) * sin(dLng / 2)
		val meters = 2 * EARTH_METERS * asin(sqrt(h))
		return BigDecimal.valueOf(meters).setScale(0, RoundingMode.HALF_UP)
	}

	private const val EARTH_METERS = 6_371_000.0
}
