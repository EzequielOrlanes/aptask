package com.aptasks.repository

import com.aptasks.model.Fine
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.Timestamp
import java.time.LocalDateTime

/** Persistência de multas com SQL declarado explicitamente. */
@Repository
class FineRepository(private val jdbcTemplate: JdbcTemplate) {
    fun insert(fine: Fine): Fine {
        jdbcTemplate.update(
            """
            INSERT INTO fine (id, timestamp, amount, sender, receiver, is_paid)
            VALUES (?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            fine.id,
            Timestamp.valueOf(LocalDateTime.parse(fine.timestamp)),
            fine.amount,
            fine.sender,
            fine.receiver,
            fine.isPaid
        )
        return fine
    }
}
