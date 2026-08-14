package com.shoptourr.insights

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class PlaceCatalogTest {

	@Test
	fun `resolves a named landmark before the trip city`() {
		val point = PlaceCatalog.resolve("Time Out Market", "Lisbon", "Portugal", "PT")

		requireNotNull(point)
		assertEquals(0, point.lat.compareTo(BigDecimal("38.706900")))
		assertEquals(0, point.lng.compareTo(BigDecimal("-9.145700")))
	}

	@Test
	fun `falls back to the trip city`() {
		val point = PlaceCatalog.resolve("Unknown cafe", "Lisbon", "Portugal", "PT")

		requireNotNull(point)
		assertEquals(0, point.lat.compareTo(BigDecimal("38.722300")))
	}

	@Test
	fun `unknown geography is absent`() {
		assertNull(PlaceCatalog.resolve("???", "Nowhereville", "Narnia", "ZZ"))
	}
}

class GeoMathTest {

	@Test
	fun `one degree of longitude at the equator is about 111 km`() {
		val from = PlaceCatalog.LatLng(BigDecimal("0.000000"), BigDecimal("0.000000"))
		val to = PlaceCatalog.LatLng(BigDecimal("0.000000"), BigDecimal("1.000000"))
		val meters = GeoMath.meters(from, to)

		assertTrue(meters in BigDecimal("110500")..BigDecimal("111700"), meters.toPlainString())
	}
}
