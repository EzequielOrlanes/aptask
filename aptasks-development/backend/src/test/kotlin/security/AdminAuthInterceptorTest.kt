package com.aptasks.security

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import com.aptasks.repository.UserRepository
import org.springframework.mock.web.MockHttpServletResponse
import java.time.Instant

class AdminAuthInterceptorTest {
    private val users = io.mockk.mockk<UserRepository>()
    private val adminUser = com.aptasks.model.User(java.util.UUID.randomUUID(), "Ezequiel", "ezequiel27.test")
    private val adminAuth = AdminAuthService("Ezequiel", "x".repeat(40), 24, users)
    private val interceptor = AdminAuthInterceptor(adminAuth)
    private lateinit var validToken: String

    @BeforeEach
    fun setUp() {
        io.mockk.every { users.findByName("Ezequiel") } returns adminUser
        validToken = adminAuth.login(adminUser.email).token
    }

    @Test
    fun `protected routes are blocked without a token`() {
        listOf(
            "POST" to "/tasks/create",
            "POST" to "/tasks/week/extend",
            "PUT" to "/tasks/123",
            "DELETE" to "/tasks/123",
            "GET" to "/fines/admin",
            "DELETE" to "/fines/123"
        ).forEach { (method, path) ->
            val response = MockHttpServletResponse()
            val allowed = interceptor.preHandle(request(method, path), response, Any())
            assertTrue(!allowed, "$method $path deveria ser bloqueado")
            assertEquals(401, response.status)
            assertTrue(response.contentAsString.contains("Acesso restrito ao administrador."))
        }
    }

    @Test
    fun `protected routes pass with a valid admin token`() {
        val response = MockHttpServletResponse()
        val allowed = interceptor.preHandle(request("DELETE", "/fines/abc", "Bearer $validToken"), response, Any())

        assertTrue(allowed)
        assertEquals(200, response.status)
    }

    @Test
    fun `a bad token is blocked`() {
        val response = MockHttpServletResponse()
        val allowed = interceptor.preHandle(request("PUT", "/tasks/abc", "Bearer lixo"), response, Any())

        assertTrue(!allowed)
        assertEquals(401, response.status)
    }

    @Test
    fun `public routes stay open for residents`() {
        listOf(
            "GET" to "/tasks/weekly",
            "PATCH" to "/tasks/123/decision",
            "POST" to "/fines",
            "GET" to "/fines",
            "PATCH" to "/fines/123/paid",
            "GET" to "/residents",
            "POST" to "/trash/half",
            "OPTIONS" to "/tasks/123"
        ).forEach { (method, path) ->
            assertTrue(interceptor.preHandle(request(method, path), MockHttpServletResponse(), Any()), "$method $path deveria ser livre")
        }
    }

    private fun request(method: String, path: String, authorization: String? = null) = MockHttpServletRequest(method, path).apply {
        if (authorization != null) addHeader("Authorization", authorization)
    }
}
