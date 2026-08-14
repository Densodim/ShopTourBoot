package com.shoptourr.media

import com.shoptourr.config.OcrProperties
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import tools.jackson.databind.JsonNode
import java.net.http.HttpClient
import java.util.Base64

@Component
class LiveOcrClient(
	properties: OcrProperties,
) {

	private val log = LoggerFactory.getLogger(javaClass)
	private val enabled = properties.enabled
	private val apiKey = properties.apiKey.trim()
	private val language = properties.language.trim().ifBlank { "eng" }
	private val maxBytes = properties.maxBytes
	private val restClient: RestClient = RestClient.builder()
		.baseUrl(properties.baseUrl.trimEnd('/'))
		.defaultHeader("Accept", "application/json")
		.requestFactory(
			JdkClientHttpRequestFactory(
				HttpClient.newBuilder().connectTimeout(properties.connectTimeout).build(),
			).also { it.setReadTimeout(properties.readTimeout) },
		)
		.build()

	fun read(contentType: String, bytes: ByteArray): String? {
		if (!enabled || apiKey.isBlank() || bytes.isEmpty() || bytes.size > maxBytes) {
			return null
		}
		if (!contentType.startsWith("image/", ignoreCase = true)) {
			return null
		}
		return try {
			val form = LinkedMultiValueMap<String, String>()
			form.add("apikey", apiKey)
			form.add("language", language)
			form.add("isOverlayRequired", "false")
			form.add("OCREngine", "2")
			form.add("scale", "true")
			form.add("base64Image", "data:$contentType;base64,${Base64.getEncoder().encodeToString(bytes)}")
			val body = restClient.post()
				.uri("/parse/image")
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.body(form)
				.retrieve()
				.body(JsonNode::class.java)
			if (body?.get("IsErroredOnProcessing")?.booleanValue() == true) {
				return null
			}
			body?.get("ParsedResults")?.get(0)?.get("ParsedText")?.stringValue()
				?.trim()
				?.takeIf { it.isNotBlank() }
		} catch (ex: RestClientException) {
			log.warn("Live OCR failed", ex)
			null
		}
	}
}
