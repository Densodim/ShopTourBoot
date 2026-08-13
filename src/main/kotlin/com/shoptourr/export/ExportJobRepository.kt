package com.shoptourr.export

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ExportJobRepository : JpaRepository<ExportJob, UUID>
