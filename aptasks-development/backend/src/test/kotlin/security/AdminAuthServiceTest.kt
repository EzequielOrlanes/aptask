package com.aptasks.security

import com.aptasks.model.User
import com.aptasks.repository.UserRepository
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant
import java.util.Base64
import java.util.UUID

class AdminAuthServiceTest {
    private val secret = "x".repeat(40)
    private val users = mockk<UserRepository>()
    private val ezequiel = User(UUID.randomUUID(), "Ezequiel", "ezequiel@example.test")
    private val service = AdminAuthService("Ezequiel", secret, 24, users)
    private val now = Instant.parse("2026-11-15T12:00:00Z")

    @BeforeEach
    fun setUp() {
        every { users.findByName("Ezequiel") } returns ezequiel
    }

    @Test
    fun `logs in when the email matches Ezequiel and returns a valid token`() {
        val session = service.login(ezequiel.email, now)

        assertTrue(service.verify(session.token, now.plusSeconds(60)))
        assertEquals(now.plusSeconds(24 * 3600).toString(), session.expiresAt)
    }

    @Test
    fun `email comparison ignores case and surrounding spaces`() {
        val session = service.login("  EZEQUIEL@example.test ", now)

        assertTrue(service.verify(session.token, now))
    }

    @Test
    fun `a different email is rejected`() {
        assertThrows<InvalidAdminCredentialsException> { service.login("outra@example.test", now) }
    }

    @Test
    fun `login is rejected when there is no Ezequiel in the users table`() {
        every { users.findByName("Ezequiel") } returns null

        assertThrows<InvalidAdminCredentialsException> { service.login(ezequiel.email, now) }
    }

    @Test
    fun `login is unavailable when the token secret is missing or too short`() {
        val shortSecret = AdminAuthService("Ezequiel", "curta", 24, users)

        assertThrows<AdminAuthUnavailableException> { shortSecret.login(ezequiel.email, now) }
    }

    @Test
    fun `expired tokens are rejected`() {
        val session = service.login(ezequiel.email, now)

        assertFalse(service.verify(session.token, now.plusSeconds(24 * 3600)))
    }

    @Test
    fun `tampered, malformed or foreign tokens are rejected`() {
        val session = service.login(ezequiel.email, now)
        val (payload, signature) = session.token.split(".")
        val forgedPayload = Base64.getUrlEncoder().withoutPadding()
            .encodeToString("Ezequiel|${now.plusSeconds(999999).epochSecond}".toByteArray())
        val otherSecret = AdminAuthService("Ezequiel", "y".repeat(40), 24, users)

        assertFalse(service.verify("$forgedPayload.$signature", now))
        assertFalse(service.verify("$payload.bad", now))
        assertFalse(service.verify("sem-ponto", now))
        assertFalse(service.verify(null, now))
        assertFalse(service.verify("", now))
        assertFalse(service.verify(otherSecret.login(ezequiel.email, now).token, now))
    }
}
