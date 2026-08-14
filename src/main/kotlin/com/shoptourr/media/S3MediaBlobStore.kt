package com.shoptourr.media

import org.slf4j.LoggerFactory
import software.amazon.awssdk.core.exception.SdkException
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.HeadObjectRequest
import software.amazon.awssdk.services.s3.model.NoSuchKeyException
import software.amazon.awssdk.services.s3.model.PutObjectRequest

class S3MediaBlobStore(
	private val s3: S3Client,
	private val bucket: String,
) : MediaBlobStore {

	private val log = LoggerFactory.getLogger(javaClass)

	override fun put(key: String, contentType: String, bytes: ByteArray) {
		try {
			s3.putObject(
				PutObjectRequest.builder()
					.bucket(bucket)
					.key(key)
					.contentType(contentType)
					.contentLength(bytes.size.toLong())
					.build(),
				RequestBody.fromBytes(bytes),
			)
		} catch (ex: SdkException) {
			log.warn("S3 put failed for key={}", key, ex)
			throw IllegalStateException("Object storage unavailable.")
		}
	}

	override fun get(key: String): ByteArray? {
		return try {
			s3.getObjectAsBytes(
				GetObjectRequest.builder().bucket(bucket).key(key).build(),
			).asByteArray()
		} catch (_: NoSuchKeyException) {
			null
		} catch (ex: SdkException) {
			log.warn("S3 get failed for key={}", key, ex)
			null
		}
	}

	override fun exists(key: String): Boolean {
		return try {
			s3.headObject(HeadObjectRequest.builder().bucket(bucket).key(key).build())
			true
		} catch (_: NoSuchKeyException) {
			false
		} catch (ex: SdkException) {
			log.warn("S3 head failed for key={}", key, ex)
			false
		}
	}

	override fun close() {
		s3.close()
	}
}
