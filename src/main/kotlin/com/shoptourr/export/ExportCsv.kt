package com.shoptourr.export

import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

internal object ExportCsv {

	fun render(purchases: List<PurchaseRow>, diary: List<DiaryRow>?): String {
		val out = StringBuilder()
		out.appendLine("id,name,category,date,time,place,gross,net,vat,vat_rate,currency,tax_refund_eligible")
		purchases.forEach { row ->
			out.appendLine(
				listOf(
					row.id.toString(),
					escape(row.name),
					escape(row.category),
					row.date.toString(),
					row.time.toString(),
					escape(row.place),
					row.gross.toPlainString(),
					row.net.toPlainString(),
					row.vat.toPlainString(),
					row.vatRate.toPlainString(),
					row.currency,
					row.taxRefundEligible.toString(),
				).joinToString(","),
			)
		}
		if (diary != null) {
			out.appendLine()
			out.appendLine("date,mood,text")
			diary.forEach { row ->
				out.appendLine(
					listOf(
						row.date.toString(),
						escape(row.mood),
						escape(row.text),
					).joinToString(","),
				)
			}
		}
		return out.toString()
	}

	internal fun escape(value: String?): String {
		val v = value.orEmpty()
		return if (v.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
			"\"${v.replace("\"", "\"\"")}\""
		} else {
			v
		}
	}

	data class PurchaseRow(
		val id: UUID,
		val name: String,
		val category: String,
		val date: LocalDate,
		val time: LocalTime,
		val place: String?,
		val gross: BigDecimal,
		val net: BigDecimal,
		val vat: BigDecimal,
		val vatRate: BigDecimal,
		val currency: String,
		val taxRefundEligible: Boolean,
	)

	data class DiaryRow(
		val date: LocalDate,
		val mood: String,
		val text: String,
	)
}
