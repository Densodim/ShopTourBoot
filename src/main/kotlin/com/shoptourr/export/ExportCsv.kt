package com.shoptourr.export

import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

internal object ExportCsv {

	fun render(
		purchases: List<PurchaseRow>,
		diary: List<DiaryRow>?,
		taxFree: TaxFreeBlock? = null,
	): String {
		val out = StringBuilder()
		val header = buildString {
			append("id,name,category,date,time,place,gross,net,vat,vat_rate,currency,tax_refund_eligible")
			if (taxFree != null) {
				append(",estimated_refund,meets_minimum")
			}
		}
		out.appendLine(header)
		purchases.forEach { row ->
			val cells = mutableListOf(
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
			)
			if (taxFree != null) {
				cells += row.estimatedRefund?.toPlainString().orEmpty()
				cells += (row.meetsMinimum ?: false).toString()
			}
			out.appendLine(cells.joinToString(","))
		}
		if (taxFree != null) {
			val eligible = purchases.filter { it.meetsMinimum == true }
			val eligibleTotal = eligible.fold(BigDecimal.ZERO) { acc, row -> acc.add(row.gross) }
			val refundTotal = eligible.fold(BigDecimal.ZERO) { acc, row ->
				acc.add(row.estimatedRefund ?: BigDecimal.ZERO)
			}
			out.appendLine()
			out.appendLine("tax_free")
			out.appendLine("currency,minimum,refund_rate,region,eligible_count,eligible_total,estimated_refund_total")
			out.appendLine(
				listOf(
					taxFree.currency,
					taxFree.minimum.toPlainString(),
					taxFree.refundRate.toPlainString(),
					escape(taxFree.region),
					eligible.size.toString(),
					eligibleTotal.toPlainString(),
					refundTotal.toPlainString(),
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
		val estimatedRefund: BigDecimal? = null,
		val meetsMinimum: Boolean? = null,
	)

	data class TaxFreeBlock(
		val currency: String,
		val minimum: BigDecimal,
		val refundRate: BigDecimal,
		val region: String,
	)

	data class DiaryRow(
		val date: LocalDate,
		val mood: String,
		val text: String,
	)
}
