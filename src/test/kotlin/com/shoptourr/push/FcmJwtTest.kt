package com.shoptourr.push

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.security.KeyPairGenerator
import java.time.Instant
import java.util.Base64

class FcmJwtTest {

	@Test
	fun `assertion is a three-part RS256 JWT`() {
		val keyGen = KeyPairGenerator.getInstance("RSA")
		keyGen.initialize(2048)
		val privateKey = keyGen.generateKeyPair().private
		val pem = "-----BEGIN PRIVATE KEY-----\n" +
			Base64.getEncoder().encodeToString(privateKey.encoded) +
			"\n-----END PRIVATE KEY-----"

		val jwt = FcmJwt.assertion(
			clientEmail = "firebase-adminsdk@demo.iam.gserviceaccount.com",
			keyId = "key-1",
			privateKeyPem = pem,
			now = Instant.parse("2026-08-13T12:00:00Z"),
			audience = "https://oauth2.googleapis.com/token",
		)

		val parts = jwt.split('.')
		assertEquals(3, parts.size)
		assertTrue(parts.all { it.isNotBlank() })
	}
}
