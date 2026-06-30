
package com.aptasks.repository

import com.aptasks.model.Fine
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface FineRepository : JpaRepository<Fine, UUID>