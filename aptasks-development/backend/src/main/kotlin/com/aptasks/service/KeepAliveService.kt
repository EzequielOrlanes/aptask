package com.aptasks.service

import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

private val logger = KotlinLogging.logger {}

/**
 * Mantém o backend acordado: faz um GET na própria URL pública, o que passa pelo proxy
 * da hospedagem e conta como tráfego de entrada. Fica desligado quando [url] está vazia.
 */
@Service
class KeepAliveService internal constructor(
    private val url: String,
    private val client: HttpClient,
) {
    @Autowired
    constructor(@Value("\${keepalive.url:}") url: String) : this(
        url,
        HttpClient.newBuilder().connectTimeout(TIMEOUT).build(),
    )

    /** Retorna o status HTTP, ou null se o ping está desligado ou falhou. Nunca lança. */
    fun ping(): Int? {
        if (url.isBlank()) return null
        return try {
            val request = HttpRequest.newBuilder(URI.create(url))
                .timeout(TIMEOUT)
                .header("X-Keep-Alive", "true")
                .GET()
                .build()
            val status = client.send(request, HttpResponse.BodyHandlers.discarding()).statusCode()
            logger.info { "Keep-alive: GET $url -> $status" }
            status
        } catch (e: Exception) {
            if (e is InterruptedException) Thread.currentThread().interrupt()
            logger.warn { "Keep-alive falhou para $url: ${e.message}" }
            null
        }
    }

    private companion object {
        val TIMEOUT: Duration = Duration.ofSeconds(10)
    }
}
