package com.aptasks.repository

import com.aptasks.dto.fine.FineResponse
import com.aptasks.model.Fine
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.sql.Timestamp
import java.time.LocalDateTime
import java.util.UUID

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
            Timestamp.valueOf(fine.timestamp),
            fine.amount,
            fine.sender,
            fine.receiver,
            fine.isPaid
        )
        return fine
    }

    fun findById(id: UUID): Fine? = jdbcTemplate.query(
        """
        SELECT id, timestamp, amount, sender, receiver, is_paid
        FROM fine
        WHERE id = ?
        """.trimIndent(),
        fineRowMapper,
        id
    ).firstOrNull()

    /** Multas em que o morador é remetente ou destinatário, mais recentes primeiro. */
    fun findByResident(userId: UUID): List<FineResponse> = jdbcTemplate.query(
        """
        SELECT f.id, f.timestamp, f.amount, f.sender, s.name AS sender_name,
               f.receiver, r.name AS receiver_name, f.is_paid
        FROM fine f
        JOIN users s ON s.id = f.sender
        JOIN users r ON r.id = f.receiver
        WHERE f.sender = ? OR f.receiver = ?
        ORDER BY f.timestamp DESC, f.id
        """.trimIndent(),
        { resultSet, _ ->
            val senderId = resultSet.getObject("sender", UUID::class.java)
            val receiverId = resultSet.getObject("receiver", UUID::class.java)
            FineResponse(
                id = resultSet.getObject("id", UUID::class.java),
                timestamp = resultSet.getTimestamp("timestamp").toLocalDateTime(),
                amount = resultSet.getBigDecimal("amount"),
                senderId = senderId,
                senderName = resultSet.getString("sender_name"),
                receiverId = receiverId,
                receiverName = resultSet.getString("receiver_name"),
                isPaid = resultSet.getBoolean("is_paid"),
                direction = if (senderId == userId) "sent" else "received"
            )
        },
        userId,
        userId
    )

    /** Marca a multa como paga. Retorna false se a multa não existe. */
    fun markPaid(id: UUID): Boolean =
        jdbcTemplate.update("UPDATE fine SET is_paid = TRUE WHERE id = ?", id) > 0

    private val fineRowMapper = RowMapper { resultSet, _ ->
        Fine(
            id = resultSet.getObject("id", UUID::class.java),
            timestamp = resultSet.getTimestamp("timestamp").toLocalDateTime(),
            amount = resultSet.getBigDecimal("amount"),
            sender = resultSet.getObject("sender", UUID::class.java),
            receiver = resultSet.getObject("receiver", UUID::class.java),
            isPaid = resultSet.getBoolean("is_paid")
        )
    }
}
