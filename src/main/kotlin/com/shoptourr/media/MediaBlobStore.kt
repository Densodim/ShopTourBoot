package com.shoptourr.media

interface MediaBlobStore : AutoCloseable {
	fun put(key: String, contentType: String, bytes: ByteArray)
	fun get(key: String): ByteArray?
	fun exists(key: String): Boolean
	override fun close() {}
}
