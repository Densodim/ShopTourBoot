package com.shoptourr.identity

import com.shoptourr.TestcontainersConfiguration
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.context.annotation.Import
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.Instant

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration::class)
@Testcontainers(disabledWithoutDocker = true)
class AppUserRepositoryTest {

	@Autowired
	private lateinit var users: AppUserRepository

	@Test
	fun `save then find by email ignores case and skips deleted`() {
		val now = Instant.parse("2026-08-13T09:00:00Z")
		val user = users.save(
			AppUser(
				email = "ada@example.com",
				passwordHash = "hash",
				displayName = "Ada",
				createdAt = now,
				updatedAt = now,
			),
		)

		assertEquals(user.id, users.findByEmailIgnoreCaseAndDeletedAtIsNull("ADA@example.com")?.id)
		assertNotNull(users.findByEmailIgnoreCaseAndDeletedAtIsNull("ada@example.com"))

		user.deletedAt = now
		users.save(user)

		assertNull(users.findByEmailIgnoreCaseAndDeletedAtIsNull("ada@example.com"))
	}

	companion object {
		@JvmStatic
		@DynamicPropertySource
		fun jwtProps(registry: DynamicPropertyRegistry) {
			registry.add("voyage.jwt.secret") { "test-only-secret-key-32bytes-min!!" }
		}
	}
}
