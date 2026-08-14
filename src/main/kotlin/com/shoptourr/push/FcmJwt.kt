package com.shoptourr.push

import java.security.KeyFactory
import java.security.Signature
import java.security.spec.PKCS8EncodedKeySpec
import java.time.Instant
import java.util.Base64

internal object FcmJwt {

	fun assertion(
		clientEmail: String,
		keyId: String,
		privateKeyPem: String,
		now: Instant,
		audience: String,
	): String {
		val header = json(mapOf("alg" to "RS256", "typ" to "JWT", "kid" to keyId))
		val payload = json(
			mapOf(
				"iss" to clientEmail,
				"scope" to SCOPE,
				"aud" to audience,
				"iat" to now.epochSecond,
				"exp" to now.epochSecond + 3600,
			),
		)
		val signingInput = "${b64(header)}.${b64(payload)}"
		val signature = sign(privateKeyPem, signingInput.toByteArray(Charsets.US_ASCII))
		return "$signingInput.${b64(signature)}"
	}

	private fun sign(pem: String, input: ByteArray): ByteArray {
		val der = Base64.getDecoder().decode(
			pem.replace("-----BEGIN PRIVATE KEY-----", "")
				.replace("-----END PRIVATE KEY-----", "")
				.replace("\\n", "")
				.replace("\n", "")
				.replace("\r", "")
				.trim(),
		)
		val key = KeyFactory.getInstance("RSA").generatePrivate(PKCS8EncodedKeySpec(der))
		val signer = Signature.getInstance("SHA256withRSA")
		signer.initSign(key)
		signer.update(input)
		return signer.sign()
	}

	private fun json(fields: Map<String, Any>): ByteArray {
		val body = fields.entries.joinToString(",", "{", "}") { (key, value) ->
			val encoded = when (value) {
				is Number -> value.toString()
				else -> "\"${value.toString().replace("\\", "\\\\").replace("\"", "\\\"")}\""
			}
			"\"$key\":$encoded"
		}
		return body.toByteArray(Charsets.UTF_8)
	}

	private fun b64(bytes: ByteArray): String =
		Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)

	private const val SCOPE = "https://www.googleapis.com/auth/firebase.messaging"
}
