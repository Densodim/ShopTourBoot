package com.shoptourr.export

import java.io.ByteArrayOutputStream
import java.util.zip.Deflater

/**
 * PDF 1.4 with an embedded DejaVu Sans CID font (Identity-H). Latin and Cyrillic
 * stay as themselves; glyphs missing from the font become `?`.
 */
internal object ExportPdf {

	private val font: EmbeddedTtf by lazy { EmbeddedTtf.load() }

	fun render(title: String, body: String): ByteArray {
		val ttf = font
		val lines = buildList {
			add(title)
			if (body.isNotBlank()) {
				add("")
				body.lineSequence().forEach { line -> addAll(wrap(line, MAX_CHARS)) }
			}
		}.ifEmpty { listOf(title) }
		val pages = lines.chunked(LINES_PER_PAGE).ifEmpty { listOf(listOf(title)) }
		val used = linkedSetOf<Int>()
		lines.forEach { line -> line.forEach { used.add(it.code) } }
		used.add(' '.code)
		used.add('?'.code)
		val pageIds = pages.indices.map { 8 + it * 2 }
		val contentIds = pages.indices.map { 9 + it * 2 }
		val compressed = flate(ttf.bytes)
		val sink = PdfSink()
		sink.raw("%PDF-1.4\n")
		sink.obj(1, "<< /Type /Catalog /Pages 2 0 R >>")
		sink.obj(
			2,
			"<< /Type /Pages /Kids [${pageIds.joinToString(" ") { "$it 0 R" }}] /Count ${pages.size} >>",
		)
		sink.obj(
			3,
			"<< /Type /Font /Subtype /Type0 /BaseFont /DejaVuSans /Encoding /Identity-H " +
				"/DescendantFonts [4 0 R] /ToUnicode 7 0 R >>",
		)
		sink.obj(
			4,
			"<< /Type /Font /Subtype /CIDFontType2 /BaseFont /DejaVuSans " +
				"/CIDSystemInfo << /Registry (Adobe) /Ordering (Identity) /Supplement 0 >> " +
				"/FontDescriptor 5 0 R /DW 600 /W ${widthArray(used, ttf)} /CIDToGIDMap /Identity >>",
		)
		sink.obj(
			5,
			"<< /Type /FontDescriptor /FontName /DejaVuSans /Flags 4 " +
				"/FontBBox [${ttf.pdf(ttf.xMin)} ${ttf.pdf(ttf.yMin)} ${ttf.pdf(ttf.xMax)} ${ttf.pdf(ttf.yMax)}] " +
				"/ItalicAngle 0 /Ascent ${ttf.pdf(ttf.ascent)} /Descent ${ttf.pdf(ttf.descent)} " +
				"/CapHeight ${ttf.pdf(ttf.capHeight)} /StemV 80 /FontFile2 6 0 R >>",
		)
		sink.stream(
			6,
			compressed,
			"<< /Filter /FlateDecode /Length ${compressed.size} /Length1 ${ttf.bytes.size} >>",
		)
		sink.stream(7, toUnicode(used, ttf).toByteArray(Charsets.US_ASCII))
		pages.forEachIndexed { index, pageLines ->
			sink.obj(
				pageIds[index],
				"<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Contents ${contentIds[index]} 0 R " +
					"/Resources << /Font << /F1 3 0 R >> >> >>",
			)
			sink.stream(contentIds[index], contentStream(pageLines, ttf))
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

	internal fun hexGids(line: String, ttf: EmbeddedTtf = font): String {
		val fallback = ttf.gid('?'.code).takeIf { it != 0 } ?: 0
		val out = StringBuilder(line.length * 4)
		line.forEach { ch ->
			val gid = ttf.gid(ch.code).takeIf { it != 0 } ?: fallback
			out.append("%04X".format(gid))
		}
		return out.toString()
	}

	private fun contentStream(lines: List<String>, ttf: EmbeddedTtf): ByteArray {
		val out = StringBuilder()
		out.append("BT\n/F1 10 Tf\n50 770 Td\n")
		lines.forEachIndexed { index, line ->
			if (index > 0) {
				out.append("0 -14 Td\n")
			}
			out.append('<').append(hexGids(line, ttf)).append("> Tj\n")
		}
		out.append("ET\n")
		return out.toString().toByteArray(Charsets.US_ASCII)
	}

	private fun widthArray(used: Set<Int>, ttf: EmbeddedTtf): String {
		val gids = used.map { code -> ttf.gid(code).takeIf { it != 0 } ?: ttf.gid('?'.code) }
			.filter { it > 0 }
			.toSortedSet()
		val out = StringBuilder("[")
		var runStart = -1
		val run = mutableListOf<Int>()
		fun flush() {
			if (runStart < 0) {
				return
			}
			out.append(' ').append(runStart).append(" [ ").append(run.joinToString(" ")).append(" ]")
			run.clear()
			runStart = -1
		}
		var prev = -2
		for (gid in gids) {
			if (gid != prev + 1) {
				flush()
				runStart = gid
			}
			run.add(ttf.width1000(gid))
			prev = gid
		}
		flush()
		out.append(" ]")
		return out.toString()
	}

	private fun toUnicode(used: Set<Int>, ttf: EmbeddedTtf): String {
		val entries = used.map { code -> ttf.gid(code) to code }
			.filter { it.first != 0 }
			.distinctBy { it.first }
			.sortedBy { it.first }
		val out = StringBuilder()
		out.append("/CIDInit /ProcSet findresource begin\n12 dict begin\nbegincmap\n")
		out.append("/CIDSystemInfo << /Registry (Adobe) /Ordering (UCS) /Supplement 0 >> def\n")
		out.append("/CMapName /Adobe-Identity-UCS def\n/CMapType 2 def\n")
		out.append("1 begincodespacerange\n<0000> <FFFF>\nendcodespacerange\n")
		entries.chunked(100).forEach { chunk ->
			out.append("${chunk.size} beginbfchar\n")
			chunk.forEach { (gid, code) ->
				out.append("<%04X> <%04X>\n".format(gid, code))
			}
			out.append("endbfchar\n")
		}
		out.append("endcmap\nCMapName currentdict /CMap defineresource pop\nend\nend\n")
		return out.toString()
	}

	private fun flate(data: ByteArray): ByteArray {
		val deflater = Deflater(Deflater.BEST_COMPRESSION)
		deflater.setInput(data)
		deflater.finish()
		val out = ByteArrayOutputStream()
		val buf = ByteArray(4096)
		while (!deflater.finished()) {
			val n = deflater.deflate(buf)
			out.write(buf, 0, n)
		}
		deflater.end()
		return out.toByteArray()
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

	fun stream(id: Int, content: ByteArray, dict: String = "<< /Length ${content.size} >>") {
		while (offsets.size < id) {
			offsets.add(-1)
		}
		offsets[id - 1] = out.size()
		raw("$id 0 obj\n$dict\nstream\n")
		out.write(content)
		raw("\nendstream\nendobj\n")
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
