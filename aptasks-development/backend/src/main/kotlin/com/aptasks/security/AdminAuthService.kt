package com.aptasks.security

import com.aptasks.repository.UserRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Instant
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Login do administrador: o e-mail digitado deve ser o do morador [adminName] na tabela
 * `users`. Passando na checagem, o backend emite um token assinado (HMAC-SHA256) usado nas
 * rotas administrativas.
 *
 * A chave de assinatura vem de ADMIN_TOKEN_SECRET quando definida. Sem ela, usa a chave
 * fixa [FALLBACK_SIGNING_KEY]. Nesse modo, quem souber o e-mail do administrador consegue
 * acessar as rotas administrativas: o login depende apenas do e-mail.
 */
@Service
class AdminAuthService(
    @Value("\${admin.name:Ezequiel}") private val adminName: String,
    @Value("\${admin.token-secret:}") private val tokenSecret: String,
    @Value("\${admin.token-ttl-hours:168}") private val tokenTtlHours: Long,
    private val userRepository: UserRepository
) {
    private val signingKey: String = tokenSecret.ifBlank { FALLBACK_SIGNING_KEY }

    companion object {
        const val FALLBACK_SIGNING_KEY = "aptasks-admin-sem-segredo-configurado"
    }

    fun login(email: String, now: Instant = Instant.now()): AdminSession {
        val admin = userRepository.findByName(adminName) ?: throw InvalidAdminCredentialsException()
        if (!admin.email.equals(email.trim(), ignoreCase = true)) throw InvalidAdminCredentialsException()

        val expiresAt = now.plusSeconds(tokenTtlHours * 3600)
        return AdminSession(token = issue(expiresAt), expiresAt = expiresAt.toString())
    }

    fun verify(token: String?, now: Instant = Instant.now()): Boolean {
        if (token.isNullOrBlank()) return false
        val parts = token.split(".")
        if (parts.size != 2) return false
        val expectedSignature = sign(parts[0])
        if (!MessageDigest.isEqual(expectedSignature.toByteArray(StandardCharsets.UTF_8), parts[1].toByteArray(StandardCharsets.UTF_8))) {
            return false
        }
        val payload = runCatching { String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8) }.getOrNull()
            ?: return false
        val expiresAt = payload.substringAfter('|', "").toLongOrNull() ?: return false
        return now.epochSecond < expiresAt && payload.substringBefore('|') == adminName
    }

    private fun issue(expiresAt: Instant): String {
        val payload = Base64.getUrlEncoder().withoutPadding()
            .encodeToString("$adminName|${expiresAt.epochSecond}".toByteArray(StandardCharsets.UTF_8))
        return "$payload.${sign(payload)}"
    }

    private fun sign(payload: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(signingKey.toByteArray(StandardCharsets.UTF_8), "HmacSHA256"))
        return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(payload.toByteArray(StandardCharsets.UTF_8)))
    }
}

data class AdminSession(val token: String, val expiresAt: String)

class InvalidAdminCredentialsException : RuntimeException("E-mail ou senha inválidos.")
