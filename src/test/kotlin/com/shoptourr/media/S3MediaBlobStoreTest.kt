package com.shoptourr.media

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.any
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import software.amazon.awssdk.core.ResponseBytes
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.GetObjectResponse
import software.amazon.awssdk.services.s3.model.HeadObjectRequest
import software.amazon.awssdk.services.s3.model.HeadObjectResponse
import software.amazon.awssdk.services.s3.model.NoSuchKeyException
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectResponse

@ExtendWith(MockitoExtension::class)
class S3MediaBlobStoreTest {

	@Mock
	private lateinit var s3: S3Client

	private lateinit var store: S3MediaBlobStore

	@BeforeEach
	fun setUp() {
		store = S3MediaBlobStore(s3, "voyage-media")
	}

	@Test
	fun `put sends the object to the bucket`() {
		`when`(s3.putObject(any(PutObjectRequest::class.java), any(RequestBody::class.java)))
			.thenReturn(PutObjectResponse.builder().build())

		store.put("media/u/id", "image/jpeg", byteArrayOf(1, 2, 3))

		verify(s3).putObject(any(PutObjectRequest::class.java), any(RequestBody::class.java))
	}

	@Test
	fun `get returns object bytes`() {
		val body = byteArrayOf(9, 8, 7)
		`when`(s3.getObjectAsBytes(any(GetObjectRequest::class.java))).thenReturn(
			ResponseBytes.fromByteArray(GetObjectResponse.builder().build(), body),
		)

		assertArrayEquals(body, store.get("media/u/id"))
	}

	@Test
	fun `get is null when the key is missing`() {
		`when`(s3.getObjectAsBytes(any(GetObjectRequest::class.java)))
			.thenThrow(NoSuchKeyException.builder().message("missing").build())

		assertNull(store.get("media/missing"))
	}

	@Test
	fun `exists is true after a successful head`() {
		`when`(s3.headObject(any(HeadObjectRequest::class.java)))
			.thenReturn(HeadObjectResponse.builder().build())

		assertTrue(store.exists("media/u/id"))
	}

	@Test
	fun `exists is false when the key is missing`() {
		`when`(s3.headObject(any(HeadObjectRequest::class.java)))
			.thenThrow(NoSuchKeyException.builder().message("missing").build())

		assertFalse(store.exists("media/missing"))
	}
}
