package com.shoptourr.identity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "app_user")
class AppUser(
	@Id
	val id: UUID = UUID.randomUUID(),

	@Column(nullable = false, length = 320)
	var email: String,

	@Column(name = "password_hash", nullable = false, length = 255)
	var passwordHash: String,

	@Column(name = "display_name", nullable = false, length = 120)
	var displayName: String,

	@Column(nullable = false, length = 5)
	var locale: String = "ru",

	@Column(name = "preferred_currency", nullable = false, length = 3)
	var preferredCurrency: String = "RUB",

	@Column(nullable = false, length = 16)
	var theme: String = "SYSTEM",

	@Column(name = "push_notifications_enabled", nullable = false)
	var pushNotificationsEnabled: Boolean = true,

	@Column(name = "dark_mode", nullable = false)
	var darkMode: Boolean = false,

	@Column(name = "avatar_media_id")
	var avatarMediaId: UUID? = null,

	@Column(name = "premium_plan", nullable = false, length = 16)
	var premiumPlan: String = "FREE",

	@Column(name = "created_at", nullable = false)
	val createdAt: Instant,

	@Column(name = "updated_at", nullable = false)
	var updatedAt: Instant,

	@Column(name = "deleted_at")
	var deletedAt: Instant? = null,
)
