package com.aptasks.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.io.IOException
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

class KeepAliveServiceTest {

    private val client = mockk<HttpClient>()

    @Test
    fun `does nothing when the url is blank`() {
        assertNull(KeepAliveService("", client).ping())

        verify(exactly = 0) { client.send(any<HttpRequest>(), any<HttpResponse.BodyHandler<Any>>()) }
    }

    @Test
    fun `returns the status of a successful ping`() {
        val response = mockk<HttpResponse<Void>>()
        every { response.statusCode() } returns 200
        every { client.send(any<HttpRequest>(), any<HttpResponse.BodyHandler<Void>>()) } returns response

        assertEquals(200, KeepAliveService("https://example.com/api/v1/hello", client).ping())
    }

    @Test
    fun `swallows network failures`() {
        every { client.send(any<HttpRequest>(), any<HttpResponse.BodyHandler<Void>>()) } throws IOException("timeout")

        assertNull(KeepAliveService("https://example.com/api/v1/hello", client).ping())
    }
}
