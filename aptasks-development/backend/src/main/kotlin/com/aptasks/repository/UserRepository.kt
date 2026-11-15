package com.aptasks.repository

import com.aptasks.model.User
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class UserRepository(private val jdbcTemplate: JdbcTemplate) {
    private val userRowMapper = RowMapper { resultSet, _ ->
        User(
            id = resultSet.getObject("id", UUID::class.java),
            name = resultSet.getString("name"),
            email = resultSet.getString("email"),
            isAtHome = resultSet.getBoolean("is_at_home"),
            homeWorkIsDone = resultSet.getBoolean("homework_is_done")
        )
    }

    fun findById(id: UUID): User? = jdbcTemplate.query(
        """
        SELECT id, name, email, is_at_home, homework_is_done
        FROM users
        WHERE id = ?
        """.trimIndent(),
        userRowMapper,
        id
    ).firstOrNull()

    /** Todos os moradores ativos no cadastro, em ordem estável para a rotação. */
    fun findAll(): List<User> = jdbcTemplate.query(
        """
        SELECT id, name, email, is_at_home, homework_is_done
        FROM users
        ORDER BY name, id
        """.trimIndent(),
        userRowMapper
    )

    fun updatePresence(id: UUID, isAtHome: Boolean): User? {
        val updatedRows = jdbcTemplate.update(
            "UPDATE users SET is_at_home = ? WHERE id = ?",
            isAtHome,
            id
        )
        return if (updatedRows == 0) null else findById(id)
    }
}
