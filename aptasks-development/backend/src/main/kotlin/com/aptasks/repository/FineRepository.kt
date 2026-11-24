package com.aptasks.repository

import com.aptasks.dto.fine.FineResponse
import com.aptasks.model.Fine
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.sql.ResultSet
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
        $SELECT_WITH_NAMES
        WHERE f.sender = ? OR f.receiver = ?
        ORDER BY f.timestamp DESC, f.id
        """.trimIndent(),
        { resultSet, _ -> mapFineResponse(resultSet, direction = directionFor(resultSet, userId)) },
        userId,
        userId
    )

    /** Todas as multas, para a área administrativa. `direction` vem como "all". */
    fun findAll(): List<FineResponse> = jdbcTemplate.query(
        """
        $SELECT_WITH_NAMES
        ORDER BY f.timestamp DESC, f.id
        """.trimIndent(),
        { resultSet, _ -> mapFineResponse(resultSet, direction = "all") }
    )

    /** Remove a multa. Retorna false se ela não existe. */
    fun deleteById(id: UUID): Boolean =
        jdbcTemplate.update("DELETE FROM fine WHERE id = ?", id) > 0

    /** Remove as multas ainda não pagas do morador criadas entre [from] e [to]. Retorna quantas foram removidas. */
    fun deleteUnpaidBetween(receiver: UUID, from: LocalDateTime, to: LocalDateTime): Int =
        jdbcTemplate.update(
            "DELETE FROM fine WHERE receiver = ? AND is_paid = FALSE AND timestamp BETWEEN ? AND ?",
            receiver,
            Timestamp.valueOf(from),
            Timestamp.valueOf(to)
        )

    /** Conta as multas já pagas do morador criadas entre [from] e [to]. */
    fun countPaidBetween(receiver: UUID, from: LocalDateTime, to: LocalDateTime): Int =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM fine WHERE receiver = ? AND is_paid = TRUE AND timestamp BETWEEN ? AND ?",
            Int::class.java,
            receiver,
            Timestamp.valueOf(from),
            Timestamp.valueOf(to)
        ) ?: 0

    /** Marca a multa como paga. Retorna false se a multa não existe. */
    fun markPaid(id: UUID): Boolean =
        jdbcTemplate.update("UPDATE fine SET is_paid = TRUE WHERE id = ?", id) > 0

    private fun directionFor(resultSet: ResultSet, userId: UUID): String =
        if (resultSet.getObject("sender", UUID::class.java) == userId) "sent" else "received"

    private fun mapFineResponse(resultSet: ResultSet, direction: String): FineResponse = FineResponse(
        id = resultSet.getObject("id", UUID::class.java),
        timestamp = resultSet.getTimestamp("timestamp").toLocalDateTime(),
        amount = resultSet.getBigDecimal("amount"),
        senderId = resultSet.getObject("sender", UUID::class.java),
        senderName = resultSet.getString("sender_name"),
        receiverId = resultSet.getObject("receiver", UUID::class.java),
        receiverName = resultSet.getString("receiver_name"),
        isPaid = resultSet.getBoolean("is_paid"),
        direction = direction
    )

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

    private companion object {
        const val SELECT_WITH_NAMES = """
            SELECT f.id, f.timestamp, f.amount, f.sender, s.name AS sender_name,
                   f.receiver, r.name AS receiver_name, f.is_paid
            FROM fine f
            JOIN users s ON s.id = f.sender
            JOIN users r ON r.id = f.receiver
        """
    }
}
