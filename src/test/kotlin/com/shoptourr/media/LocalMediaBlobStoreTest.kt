package com.shoptourr.media

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class LocalMediaBlobStoreTest {

	@TempDir
	lateinit var root: Path

	@Test
	fun `put then get round-trips bytes`() {
		val store = LocalMediaBlobStore(root)
		val key = "media/11111111-1111-1111-1111-111111111111/22222222-2222-2222-2222-222222222222"
		val body = byteArrayOf(1, 2, 3, 4)

		store.put(key, "image/jpeg", body)

		assertTrue(store.exists(key))
		assertArrayEquals(body, store.get(key))
	}

	@Test
	fun `append concatenates onto an existing object`() {
		val store = LocalMediaBlobStore(root)
		val key = "media/11111111-1111-1111-1111-111111111111/22222222-2222-2222-2222-222222222222"

		store.put(key, "image/jpeg", byteArrayOf(1, 2))
		store.append(key, "image/jpeg", byteArrayOf(3, 4))

		assertArrayEquals(byteArrayOf(1, 2, 3, 4), store.get(key))
	}

	@Test
	fun `missing keys are absent`() {
		val store = LocalMediaBlobStore(root)

		assertFalse(store.exists("media/missing"))
		assertNull(store.get("media/missing"))
	}

	@Test
	fun `rejects a path-escape key`() {
		val store = LocalMediaBlobStore(root)

		org.junit.jupiter.api.assertThrows<IllegalArgumentException> {
			store.put("../secret", "text/plain", byteArrayOf(1))
		}
	}
}
