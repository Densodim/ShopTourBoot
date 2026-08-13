package com.shoptourr.trip

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "traveler")
class Traveler(
	@Id
	val id: UUID = UUID.randomUUID(),

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "trip_id", nullable = false)
	var trip: Trip,

	@Column(name = "user_id")
	val userId: UUID? = null,

	@Column(nullable = false, length = 60)
	var name: String,

	@Column(name = "color_hex", nullable = false, length = 7)
	var colorHex: String,

	@Column(name = "avatar_glyph", length = 2)
	var avatarGlyph: String? = null,

	@Column(name = "is_owner", nullable = false)
	val isOwner: Boolean = false,

	@Column(name = "created_at", nullable = false)
	val createdAt: Instant,

	@Column(name = "deleted_at")
	var deletedAt: Instant? = null,
)
