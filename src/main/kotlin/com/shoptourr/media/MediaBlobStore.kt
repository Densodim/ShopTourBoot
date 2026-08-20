package com.shoptourr.media

interface MediaBlobStore : AutoCloseable {
	fun put(key: String, contentType: String, bytes: ByteArray)
	fun get(key: String): ByteArray?
	fun exists(key: String): Boolean
	fun append(key: String, contentType: String, bytes: ByteArray) {
		val existing = get(key) ?: ByteArray(0)
		put(key, contentType, existing + bytes)
	}
	fun delete(key: String) {}
	override fun close() {}
}
