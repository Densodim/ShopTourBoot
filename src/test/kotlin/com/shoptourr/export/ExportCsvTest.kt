package com.shoptourr.export

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

class ExportCsvTest {

	@Test
	fun `quotes commas and doubled quotes`() {
		assertEquals("plain", ExportCsv.escape("plain"))
		assertEquals("\"a,b\"", ExportCsv.escape("a,b"))
		assertEquals("\"say \"\"hi\"\"\"", ExportCsv.escape("say \"hi\""))
	}

	@Test
	fun `renders purchase rows and optional diary section`() {
		val purchaseId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa")
		val csv = ExportCsv.render(
			purchases = listOf(
				ExportCsv.PurchaseRow(
					id = purchaseId,
					name = "Coffee, large",
					category = "FOOD",
					date = LocalDate.parse("2026-08-12"),
					time = LocalTime.parse("09:15:00"),
					place = "Cafe",
					gross = BigDecimal("3.50"),
					net = BigDecimal("2.85"),
					vat = BigDecimal("0.65"),
					vatRate = BigDecimal("23.00"),
					currency = "EUR",
					taxRefundEligible = false,
				),
			),
			diary = listOf(
				ExportCsv.DiaryRow(LocalDate.parse("2026-08-12"), "GOOD", "Walked the city"),
			),
		)

		assertTrue(csv.startsWith("id,name,category,date,time,place,gross,net,vat,vat_rate,currency,tax_refund_eligible\n"))
		assertTrue(csv.contains("\"Coffee, large\""))
		assertTrue(csv.contains("\ndate,mood,text\n"))
		assertTrue(csv.contains("2026-08-12,GOOD,Walked the city"))
	}

	@Test
	fun `omits diary section when diary is null`() {
		val csv = ExportCsv.render(purchases = emptyList(), diary = null)

		assertEquals(
			"id,name,category,date,time,place,gross,net,vat,vat_rate,currency,tax_refund_eligible\n",
			csv,
		)
	}

	@Test
	fun `adds tax-free columns and summary when requested`() {
		val purchaseId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa")
		val csv = ExportCsv.render(
			purchases = listOf(
				ExportCsv.PurchaseRow(
					id = purchaseId,
					name = "Watch",
					category = "SHOPPING",
					date = LocalDate.parse("2026-08-12"),
					time = LocalTime.parse("11:00:00"),
					place = "Chiado",
					gross = BigDecimal("123.00"),
					net = BigDecimal("100.00"),
					vat = BigDecimal("23.00"),
					vatRate = BigDecimal("23.00"),
					currency = "EUR",
					taxRefundEligible = true,
					estimatedRefund = BigDecimal("15.99"),
					meetsMinimum = true,
				),
			),
			diary = null,
			taxFree = ExportCsv.TaxFreeBlock(
				currency = "EUR",
				minimum = BigDecimal("50.00"),
				refundRate = BigDecimal("0.13"),
				region = "Portugal",
			),
		)

		assertTrue(
			csv.startsWith(
				"id,name,category,date,time,place,gross,net,vat,vat_rate,currency,tax_refund_eligible,estimated_refund,meets_minimum\n",
			),
		)
		assertTrue(csv.contains("123.00,100.00,23.00,23.00,EUR,true,15.99,true"))
		assertTrue(csv.contains("\ntax_free\n"))
		assertTrue(csv.contains("EUR,50.00,0.13,Portugal,1,123.00,15.99"))
	}
}
