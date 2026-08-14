package com.shoptourr.media

import com.shoptourr.config.S3Properties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.S3Configuration
import java.net.URI

@Configuration
class MediaBlobConfig {

	@Bean(destroyMethod = "close")
	fun mediaBlobStore(properties: S3Properties): MediaBlobStore {
		if (!properties.enabled) {
			return LocalMediaBlobStore(properties.localRoot())
		}
		return S3MediaBlobStore(s3Client(properties), properties.bucket.trim())
	}

	private fun s3Client(properties: S3Properties): S3Client {
		val builder = S3Client.builder()
			.region(Region.of(properties.region.trim().ifBlank { "eu-central-1" }))
			.httpClientBuilder(UrlConnectionHttpClient.builder())
			.serviceConfiguration(
				S3Configuration.builder()
					.pathStyleAccessEnabled(properties.pathStyle)
					.build(),
			)
		val endpoint = properties.endpoint.trim()
		if (endpoint.isNotEmpty()) {
			builder.endpointOverride(URI.create(endpoint))
		}
		if (properties.accessKey.isNotBlank()) {
			builder.credentialsProvider(
				StaticCredentialsProvider.create(
					AwsBasicCredentials.create(properties.accessKey, properties.secretKey),
				),
			)
		}
		return builder.build()
	}
}
