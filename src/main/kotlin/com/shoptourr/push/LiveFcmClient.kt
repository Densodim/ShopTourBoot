package com.shoptourr.push

import com.shoptourr.config.FcmProperties
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestClientResponseException
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper
import java.net.http.HttpClient
import java.nio.file.Files
import java.nio.file.Path
import java.time.Clock
import java.time.Instant

enum class FcmSendResult {
	SENT, SKIPPED, UNREGISTERED, FAILED
}

@Component
class LiveFcmClient(
	private val properties: FcmProperties,
	private val jsonMapper: JsonMapper,
	private val clock: Clock,
) {

	private val log = LoggerFactory.getLogger(javaClass)
	private val restClient: RestClient = RestClient.builder()
		.requestFactory(
			JdkClientHttpRequestFactory(
				HttpClient.newBuilder().connectTimeout(properties.connectTimeout).build(),
			).also { it.setReadTimeout(properties.readTimeout) },
		)
		.build()

	@Volatile
	private var cachedToken: String? = null

	@Volatile
	private var cachedUntil: Instant = Instant.EPOCH

	fun send(token: String, title: String, body: String, data: Map<String, String>): FcmSendResult {
		if (!properties.enabled || token.isBlank()) {
			return FcmSendResult.SKIPPED
		}
		val account = account() ?: return FcmSendResult.SKIPPED
		val access = accessToken(account) ?: return FcmSendResult.FAILED
		val url = properties.sendUrl.replace("{projectId}", account.projectId)
		return try {
			restClient.post()
				.uri(url)
				.header("Authorization", "Bearer $access")
				.contentType(MediaType.APPLICATION_JSON)
				.body(
					mapOf(
						"message" to mapOf(
							"token" to token,
							"notification" to mapOf("title" to title, "body" to body),
							"data" to data,
							"android" to mapOf("priority" to "HIGH"),
						),
					),
				)
				.retrieve()
				.toBodilessEntity()
			FcmSendResult.SENT
		} catch (ex: RestClientResponseException) {
			val payload = ex.responseBodyAsString
			if (payload.contains("UNREGISTERED") || ex.statusCode.value() == 404) {
				FcmSendResult.UNREGISTERED
			} else {
				log.warn("FCM send failed status={}", ex.statusCode.value())
				FcmSendResult.FAILED
			}
		} catch (ex: RestClientException) {
			log.warn("FCM send failed", ex)
			FcmSendResult.FAILED
		}
	}

	private fun accessToken(account: ServiceAccount): String? {
		val now = Instant.now(clock)
		val hit = cachedToken
		if (hit != null && now.isBefore(cachedUntil)) {
			return hit
		}
		return try {
			val assertion = FcmJwt.assertion(
				clientEmail = account.clientEmail,
				keyId = account.privateKeyId,
				privateKeyPem = account.privateKey,
				now = now,
				audience = properties.tokenUrl,
			)
			val form = LinkedMultiValueMap<String, String>()
			form.add("grant_type", "urn:ietf:params:oauth:grant-type:jwt-bearer")
			form.add("assertion", assertion)
			val body = restClient.post()
				.uri(properties.tokenUrl)
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.body(form)
				.retrieve()
				.body(JsonNode::class.java)
			val token = body?.get("access_token")?.stringValue()?.takeIf { it.isNotBlank() } ?: return null
			val ttl = body.get("expires_in")?.intValue()?.takeIf { it > 120 } ?: 3600
			cachedToken = token
			cachedUntil = now.plusSeconds((ttl - 60).toLong())
			token
		} catch (ex: RestClientException) {
			log.warn("FCM access token failed", ex)
			null
		} catch (ex: IllegalArgumentException) {
			log.warn("FCM credentials are invalid")
			null
		}
	}

	private fun account(): ServiceAccount? {
		val raw = properties.credentials.trim()
		if (raw.isEmpty()) {
			return null
		}
		val json = if (raw.startsWith("{")) {
			raw
		} else {
			val path = Path.of(raw)
			if (!Files.isRegularFile(path)) {
				log.warn("FCM credentials file is missing")
				return null
			}
			Files.readString(path)
		}
		return try {
			val node = jsonMapper.readTree(json)
			val projectId = node.get("project_id")?.stringValue()?.trim().orEmpty()
			val clientEmail = node.get("client_email")?.stringValue()?.trim().orEmpty()
			val privateKey = node.get("private_key")?.stringValue().orEmpty()
			val keyId = node.get("private_key_id")?.stringValue().orEmpty()
			if (projectId.isBlank() || clientEmail.isBlank() || privateKey.isBlank()) {
				null
			} else {
				ServiceAccount(projectId, clientEmail, privateKey, keyId)
			}
		} catch (ex: tools.jackson.core.JacksonException) {
			log.warn("FCM credentials JSON is invalid")
			null
		}
	}

	private data class ServiceAccount(
		val projectId: String,
		val clientEmail: String,
		val privateKey: String,
		val privateKeyId: String,
	)
}
