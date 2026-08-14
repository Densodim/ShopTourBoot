package com.shoptourr.media

import com.shoptourr.DomainValidationException
import com.shoptourr.MediaNotReadyException
import com.shoptourr.config.MediaProperties
import com.shoptourr.media.dto.ConfirmMediaUploadRequest
import com.shoptourr.media.dto.MediaPurpose
import com.shoptourr.media.dto.MediaStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import java.security.MessageDigest
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@ExtendWith(MockitoExtension::class)
class MediaServiceTest {

	@Mock
	private lateinit var assets: MediaAssetRepository

	private val clock = Clock.fixed(Instant.parse("2026-08-13T12:00:00Z"), ZoneOffset.UTC)
	private lateinit var blobs: InMemoryMediaBlobStore
	private lateinit var service: MediaService
	private val userId = UUID.fromString("11111111-1111-1111-1111-111111111111")

	@BeforeEach
	fun setUp() {
		blobs = InMemoryMediaBlobStore()
		service = MediaService(assets, blobs, MediaProperties(), clock)
	}

	@Test
	fun `confirm without stored bytes is not ready`() {
		val asset = asset(status = MediaStatus.PENDING_UPLOAD.name, content = null)
		`when`(assets.findByIdAndDeletedAtIsNull(asset.id)).thenReturn(asset)

		assertThrows<MediaNotReadyException> {
			service.confirm(userId, asset.id, ConfirmMediaUploadRequest(uploaded = true))
		}
	}

	@Test
	fun `confirm after upload marks the asset ready`() {
		val payload = byteArrayOf(1, 2, 3, 4)
		val asset = asset(status = MediaStatus.UPLOADED.name)
		val key = MediaService.keyFor(asset)
		asset.storageKey = key
		blobs.put(key, asset.contentType, payload)
		`when`(assets.findByIdAndDeletedAtIsNull(asset.id)).thenReturn(asset)

		val dto = service.confirm(userId, asset.id, ConfirmMediaUploadRequest(uploaded = true))

		assertEquals(MediaStatus.READY, dto.status)
		assertEquals(MediaStatus.READY.name, asset.status)
	}

	@Test
	fun `storeBytes rejects a sha256 mismatch`() {
		val payload = byteArrayOf(1, 2, 3, 4)
		val asset = asset(status = MediaStatus.PENDING_UPLOAD.name, sha256Hex = "0".repeat(64))
		`when`(assets.findByIdAndDeletedAtIsNull(asset.id)).thenReturn(asset)

		assertThrows<DomainValidationException> {
			service.storeBytes(asset.id, payload)
		}
	}

	@Test
	fun `storeBytes accepts a matching sha256`() {
		val payload = byteArrayOf(1, 2, 3, 4)
		val asset = asset(status = MediaStatus.PENDING_UPLOAD.name, sha256Hex = sha256Hex(payload))
		`when`(assets.findByIdAndDeletedAtIsNull(asset.id)).thenReturn(asset)

		service.storeBytes(asset.id, payload)

		assertEquals(MediaStatus.UPLOADED.name, asset.status)
		assertEquals(payload.size.toLong(), asset.byteSize)
		assertEquals(MediaService.keyFor(asset), asset.storageKey)
		assertEquals(null, asset.content)
		assertTrue(blobs.exists(MediaService.keyFor(asset)))
	}

	@Test
	fun `storeBytes rejects a confirmed asset`() {
		val asset = asset(status = MediaStatus.READY.name, content = byteArrayOf(1))
		`when`(assets.findByIdAndDeletedAtIsNull(asset.id)).thenReturn(asset)

		assertThrows<DomainValidationException> {
			service.storeBytes(asset.id, byteArrayOf(9, 8, 7))
		}
	}

	@Test
	fun `publicUrlIfReady is null until the asset is ready`() {
		val pending = asset(status = MediaStatus.PENDING_UPLOAD.name)
		`when`(assets.findByIdAndDeletedAtIsNull(pending.id)).thenReturn(pending)

		assertEquals(null, service.publicUrlIfReady(userId, pending.id))
	}

	@Test
	fun `publicUrlIfReady returns the download path for a ready asset`() {
		val ready = asset(status = MediaStatus.READY.name, content = byteArrayOf(1))
		`when`(assets.findByIdAndDeletedAtIsNull(ready.id)).thenReturn(ready)

		assertEquals("http://localhost:8080/dev-uploads/${ready.id}", service.publicUrlIfReady(userId, ready.id))
	}

	@Test
	fun `ocr reads structured bytes from a ready receipt`() {
		val body = "name: Pastel\namount: 1.20\ncategory: FOOD".toByteArray()
		val asset = asset(status = MediaStatus.READY.name, contentType = "text/plain")
		val key = MediaService.keyFor(asset)
		asset.storageKey = key
		blobs.put(key, asset.contentType, body)
		`when`(assets.findByIdAndDeletedAtIsNull(asset.id)).thenReturn(asset)

		val result = service.ocr(userId, asset.id)

		assertEquals("Pastel", result.suggestedName)
		assertEquals("1.20", result.suggestedAmount)
		assertEquals("FOOD", result.suggestedCategory)
	}

	@Test
	fun `legacy bytea content is still readable`() {
		val payload = byteArrayOf(4, 5, 6)
		val asset = asset(status = MediaStatus.UPLOADED.name, content = payload)
		`when`(assets.findByIdAndDeletedAtIsNull(asset.id)).thenReturn(asset)

		val stored = service.loadBytes(asset.id)

		assertEquals("image/jpeg", stored.contentType)
		assertTrue(payload.contentEquals(stored.bytes))
	}

	private fun asset(
		status: String,
		content: ByteArray? = null,
		sha256Hex: String? = null,
		contentType: String = "image/jpeg",
	) = MediaAsset(
		userId = userId,
		purpose = MediaPurpose.RECEIPT.name,
		status = status,
		contentType = contentType,
		byteSize = content?.size?.toLong() ?: 4,
		sha256Hex = sha256Hex,
		content = content,
		createdAt = Instant.now(clock),
		updatedAt = Instant.now(clock),
	)

	private fun sha256Hex(body: ByteArray): String =
		MessageDigest.getInstance("SHA-256").digest(body).joinToString("") { byte -> "%02x".format(byte) }
}

internal class InMemoryMediaBlobStore : MediaBlobStore {
	private val items = ConcurrentHashMap<String, ByteArray>()

	override fun put(key: String, contentType: String, bytes: ByteArray) {
		items[key] = bytes.copyOf()
	}

	override fun get(key: String): ByteArray? = items[key]?.copyOf()

	override fun exists(key: String): Boolean = items.containsKey(key)
}
