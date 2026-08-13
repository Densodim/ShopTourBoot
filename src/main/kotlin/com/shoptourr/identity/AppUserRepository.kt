package com.shoptourr.identity

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface AppUserRepository : JpaRepository<AppUser, UUID> {

	fun findByEmailIgnoreCaseAndDeletedAtIsNull(email: String): AppUser?
}
