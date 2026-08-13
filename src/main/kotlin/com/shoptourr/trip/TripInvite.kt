package com.shoptourr.trip

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "trip_invite")
class TripInvite(
	@Id
	val id: UUID = UUID.randomUUID(),

	@Column(name = "trip_id", nullable = false)
	val tripId: UUID,

	@Column(nullable = false, length = 320)
	val email: String,

	@Column(name = "display_name_hint", length = 60)
	val displayNameHint: String? = null,

	@Column(nullable = false, length = 16)
	var status: String,

	@Column(name = "created_at", nullable = false)
	val createdAt: Instant,

	@Column(name = "expires_at", nullable = false)
	val expiresAt: Instant,
)
