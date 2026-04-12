package com.aptasks.service

import com.aptasks.config.SupabaseConfig
import com.aptasks.dto.LoginRequest
import com.aptasks.dto.LoginResponse
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.springframework.stereotype.Service

@Service
class AuthService(
    private val supabaseConfig: SupabaseConfig,  
    private val httpClient: OkHttpClient,
    private val objectMapper: ObjectMapper
) {
    
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    
    fun login(loginRequest: LoginRequest): LoginResponse {
        val loginJson = objectMapper.writeValueAsString(
            mapOf(
                "email" to loginRequest.email,
                "password" to loginRequest.password
            )
        )
        
        val requestBody = loginJson.toRequestBody(jsonMediaType)
        
        val request = Request.Builder()
            .url("${supabaseConfig.supabaseUrl}/auth/v1/token?grant_type=password")
            .addHeader("apikey", supabaseConfig.supabaseKey)
            .addHeader("Content-Type", "application/json")
            .post(requestBody)
            .build()
        
        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: "Erro desconhecido"
                throw RuntimeException("Falha na autenticação: $errorBody")
            }
            
            val body = response.body?.string() ?: throw RuntimeException("Resposta vazia")
            return objectMapper.readValue(body)
        }
    }
}
