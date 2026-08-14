package com.shoptourr.export

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Enough of a TrueType file to embed as a PDF CIDFontType2: cmap, hmtx, and
 * descriptor metrics. The original bytes are kept so they can be written as FontFile2.
 */
internal class EmbeddedTtf(
	val bytes: ByteArray,
	val unitsPerEm: Int,
	val xMin: Int,
	val yMin: Int,
	val xMax: Int,
	val yMax: Int,
	val ascent: Int,
	val descent: Int,
	val capHeight: Int,
	private val cmap: Map<Int, Int>,
	private val widths: IntArray,
) {

	fun gid(codePoint: Int): Int = cmap[codePoint] ?: 0

	fun width1000(gid: Int): Int {
		if (gid !in widths.indices) {
			return 600
		}
		return (widths[gid] * 1000 + unitsPerEm / 2) / unitsPerEm
	}

	fun pdf(units: Int): Int = (units * 1000 + unitsPerEm / 2) / unitsPerEm

	companion object {
		fun load(): EmbeddedTtf {
			val stream = requireNotNull(EmbeddedTtf::class.java.getResourceAsStream("/fonts/DejaVuSans.ttf")) {
				"DejaVuSans.ttf is missing from the classpath."
			}
			return parse(stream.use { it.readBytes() })
		}

		internal fun parse(bytes: ByteArray): EmbeddedTtf {
			val file = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN)
			val tables = tables(file)
			val head = requireTable(file, tables, "head")
			val hhea = requireTable(file, tables, "hhea")
			val maxp = requireTable(file, tables, "maxp")
			val hmtx = requireTable(file, tables, "hmtx")
			val cmap = requireTable(file, tables, "cmap")
			val unitsPerEm = u16(head, 18)
			val numGlyphs = u16(maxp, 4)
			val numberOfHMetrics = u16(hhea, 34)
			val widths = IntArray(numGlyphs)
			var cursor = 0
			for (i in 0 until numberOfHMetrics) {
				widths[i] = u16(hmtx, cursor)
				cursor += 4
			}
			val last = widths[numberOfHMetrics - 1]
			for (i in numberOfHMetrics until numGlyphs) {
				widths[i] = last
			}
			val os2 = table(file, tables, "OS/2")
			val capHeight = if (os2 != null && os2.remaining() >= 90 && u16(os2, 0) >= 2) {
				i16(os2, 88)
			} else {
				i16(hhea, 4)
			}
			return EmbeddedTtf(
				bytes = bytes,
				unitsPerEm = unitsPerEm,
				xMin = i16(head, 36),
				yMin = i16(head, 38),
				xMax = i16(head, 40),
				yMax = i16(head, 42),
				ascent = i16(hhea, 4),
				descent = i16(hhea, 6),
				capHeight = capHeight,
				cmap = parseCmap(cmap),
				widths = widths,
			)
		}

		private fun tables(file: ByteBuffer): Map<String, IntArray> {
			val numTables = u16(file, 4)
			val out = linkedMapOf<String, IntArray>()
			var offset = 12
			repeat(numTables) {
				val tag = tag(file, offset)
				out[tag] = intArrayOf(u32(file, offset + 8), u32(file, offset + 12))
				offset += 16
			}
			return out
		}

		private fun requireTable(file: ByteBuffer, tables: Map<String, IntArray>, tag: String): ByteBuffer =
			requireNotNull(table(file, tables, tag)) { "TTF is missing table $tag" }

		private fun table(file: ByteBuffer, tables: Map<String, IntArray>, tag: String): ByteBuffer? {
			val loc = tables[tag] ?: return null
			return file.duplicate().order(ByteOrder.BIG_ENDIAN).apply {
				position(loc[0])
				limit(loc[0] + loc[1])
			}
		}

		private fun parseCmap(cmap: ByteBuffer): Map<Int, Int> {
			val numTables = u16(cmap, 2)
			var best: ByteBuffer? = null
			var bestScore = -1
			var rec = 4
			repeat(numTables) {
				val platform = u16(cmap, rec)
				val encoding = u16(cmap, rec + 2)
				val subOffset = u32(cmap, rec + 4)
				val sub = cmap.duplicate().order(ByteOrder.BIG_ENDIAN).apply {
					position(cmap.position() + subOffset)
					limit(cmap.limit())
				}
				val format = u16(sub, 0)
				val score = when {
					platform == 3 && encoding == 10 && format == 12 -> 40
					platform == 0 && format == 12 -> 30
					platform == 3 && encoding == 1 && format == 4 -> 20
					platform == 0 && format == 4 -> 10
					else -> -1
				}
				if (score > bestScore) {
					bestScore = score
					best = sub
				}
				rec += 8
			}
			val chosen = best ?: return emptyMap()
			return when (u16(chosen, 0)) {
				4 -> parseFormat4(chosen)
				12 -> parseFormat12(chosen)
				else -> emptyMap()
			}
		}

		private fun parseFormat4(table: ByteBuffer): Map<Int, Int> {
			val segCount = u16(table, 6) / 2
			val endCodes = 14
			val startCodes = endCodes + 2 * segCount + 2
			val idDeltas = startCodes + 2 * segCount
			val idRangeOffsets = idDeltas + 2 * segCount
			val map = HashMap<Int, Int>()
			for (i in 0 until segCount) {
				val start = u16(table, startCodes + i * 2)
				val end = u16(table, endCodes + i * 2)
				val delta = i16(table, idDeltas + i * 2)
				val rangeOffset = u16(table, idRangeOffsets + i * 2)
				for (code in start..end) {
					val gid = if (rangeOffset == 0) {
						(code + delta) and 0xFFFF
					} else {
						val glyphOffset = idRangeOffsets + i * 2 + rangeOffset + (code - start) * 2
						val glyphId = u16(table, glyphOffset)
						if (glyphId == 0) 0 else (glyphId + delta) and 0xFFFF
					}
					if (gid != 0) {
						map[code] = gid
					}
				}
			}
			return map
		}

		private fun parseFormat12(table: ByteBuffer): Map<Int, Int> {
			val nGroups = u32(table, 12)
			val map = HashMap<Int, Int>()
			var cursor = 16
			repeat(nGroups) {
				val startChar = u32(table, cursor)
				val endChar = u32(table, cursor + 4)
				val startGlyph = u32(table, cursor + 8)
				for (code in startChar..endChar) {
					map[code] = startGlyph + (code - startChar)
				}
				cursor += 12
			}
			return map
		}

		private fun u16(buf: ByteBuffer, offset: Int): Int = buf.getShort(buf.position() + offset).toInt() and 0xFFFF

		private fun i16(buf: ByteBuffer, offset: Int): Int = buf.getShort(buf.position() + offset).toInt()

		private fun u32(buf: ByteBuffer, offset: Int): Int = buf.getInt(buf.position() + offset)

		private fun tag(buf: ByteBuffer, offset: Int): String {
			val at = buf.position() + offset
			return String(byteArrayOf(buf.get(at), buf.get(at + 1), buf.get(at + 2), buf.get(at + 3)), Charsets.ISO_8859_1)
		}
	}
}
