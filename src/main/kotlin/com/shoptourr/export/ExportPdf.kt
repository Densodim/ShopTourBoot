package com.shoptourr.export

import java.io.ByteArrayOutputStream

/**
 * Minimal single-font PDF for local downloads. No third-party renderer:
 * Courier + WinAnsi, non-ASCII replaced with `?`.
 */
internal object ExportPdf {

	fun render(title: String, body: String): ByteArray {
		val lines = buildList {
			add(title)
			if (body.isNotBlank()) {
				add("")
				body.lineSequence().forEach { line -> addAll(wrap(line, MAX_CHARS)) }
			}
		}.ifEmpty { listOf(title) }
		val pages = lines.chunked(LINES_PER_PAGE).ifEmpty { listOf(listOf(title)) }
		val fontId = 3
		val pageIds = pages.indices.map { 4 + it * 2 }
		val contentIds = pages.indices.map { 5 + it * 2 }
		val sink = PdfSink()
		sink.raw("%PDF-1.4\n")
		sink.obj(1, "<< /Type /Catalog /Pages 2 0 R >>")
		sink.obj(
			2,
			"<< /Type /Pages /Kids [${pageIds.joinToString(" ") { "$it 0 R" }}] /Count ${pages.size} >>",
		)
		sink.obj(3, "<< /Type /Font /Subtype /Type1 /BaseFont /Courier >>")
		pages.forEachIndexed { index, pageLines ->
			sink.obj(
				pageIds[index],
				"<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Contents ${contentIds[index]} 0 R " +
					"/Resources << /Font << /F1 $fontId 0 R >> >> >>",
			)
			sink.stream(contentIds[index], contentStream(pageLines))
		}
		sink.finish(rootId = 1)
		return sink.toByteArray()
	}

	internal fun wrap(line: String, width: Int): List<String> {
		if (line.length <= width) {
			return listOf(line.ifEmpty { " " })
		}
		return line.chunked(width)
	}

	internal fun escape(text: String): String {
		val out = StringBuilder(text.length)
		text.forEach { ch ->
			when {
				ch == '\\' || ch == '(' || ch == ')' -> {
					out.append('\\')
					out.append(ch)
				}
				ch == '\n' || ch == '\r' || ch == '\t' -> out.append(' ')
				ch.code in 32..126 -> out.append(ch)
				else -> out.append('?')
			}
		}
		return out.toString()
	}

	private fun contentStream(lines: List<String>): ByteArray {
		val out = StringBuilder()
		out.append("BT\n/F1 10 Tf\n50 770 Td\n")
		lines.forEachIndexed { index, line ->
			if (index > 0) {
				out.append("0 -14 Td\n")
			}
			out.append('(').append(escape(line)).append(") Tj\n")
		}
		out.append("ET\n")
		return out.toString().toByteArray(Charsets.US_ASCII)
	}

	private const val MAX_CHARS = 90
	private const val LINES_PER_PAGE = 50
}

private class PdfSink {
	private val out = ByteArrayOutputStream()
	private val offsets = mutableListOf<Int>()

	fun raw(text: String) {
		out.write(text.toByteArray(Charsets.US_ASCII))
	}

	fun obj(id: Int, body: String) {
		while (offsets.size < id) {
			offsets.add(-1)
		}
		offsets[id - 1] = out.size()
		raw("$id 0 obj\n$body\nendobj\n")
	}

	fun stream(id: Int, content: ByteArray) {
		while (offsets.size < id) {
			offsets.add(-1)
		}
		offsets[id - 1] = out.size()
		raw("$id 0 obj\n<< /Length ${content.size} >>\nstream\n")
		out.write(content)
		raw("endstream\nendobj\n")
	}

	fun finish(rootId: Int) {
		val start = out.size()
		val size = offsets.size + 1
		raw("xref\n0 $size\n")
		raw("0000000000 65535 f \n")
		offsets.forEach { offset ->
			raw("%010d 00000 n \n".format(offset))
		}
		raw("trailer << /Size $size /Root $rootId 0 R >>\nstartxref\n$start\n%%EOF\n")
	}

	fun toByteArray(): ByteArray = out.toByteArray()
}
