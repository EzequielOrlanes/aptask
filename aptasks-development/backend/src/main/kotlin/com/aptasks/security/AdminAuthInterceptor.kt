package com.aptasks.security

import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.util.AntPathMatcher
import org.springframework.web.servlet.HandlerInterceptor
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse

/** Exige token de administrador nas rotas de escrita administrativa. */
@Component
class AdminAuthInterceptor(private val adminAuth: AdminAuthService) : HandlerInterceptor {
    private val matcher = AntPathMatcher()

    companion object {
        /** Método + padrão de caminho das rotas que só o administrador acessa. */
        val PROTECTED_ROUTES = listOf(
            "POST" to "/tasks/create",
            "PUT" to "/tasks/*",
            "DELETE" to "/tasks/*",
            "GET" to "/fines/admin",
            "DELETE" to "/fines/*"
        )
    }

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        if (request.method == "OPTIONS") return true
        val protected = PROTECTED_ROUTES.any { (method, pattern) ->
            method == request.method && matcher.match(pattern, request.requestURI)
        }
        if (!protected) return true

        val header = request.getHeader("Authorization")
        val token = header?.takeIf { it.startsWith("Bearer ") }?.removePrefix("Bearer ")
        if (adminAuth.verify(token)) return true

        response.status = HttpStatus.UNAUTHORIZED.value()
        response.contentType = "application/json;charset=UTF-8"
        response.writer.write("""{"success":false,"error":"Acesso restrito ao administrador."}""")
        return false
    }
}
