package com.shoptourr.media

import java.nio.file.Files
import java.nio.file.Path

class LocalMediaBlobStore(
	root: Path,
) : MediaBlobStore {

	private val root = root.toAbsolutePath().normalize()

	init {
		Files.createDirectories(this.root)
	}

	override fun put(key: String, contentType: String, bytes: ByteArray) {
		val path = resolve(key)
		Files.createDirectories(path.parent)
		Files.write(path, bytes)
	}

	override fun get(key: String): ByteArray? {
		val path = resolve(key)
		if (!Files.isRegularFile(path)) {
			return null
		}
		return Files.readAllBytes(path)
	}

	override fun exists(key: String): Boolean = Files.isRegularFile(resolve(key))

	private fun resolve(key: String): Path {
		require(key.isNotBlank() && !key.contains("..")) { "Invalid storage key." }
		val path = root.resolve(key).normalize()
		require(path.startsWith(root)) { "Invalid storage key." }
		return path
	}
}
