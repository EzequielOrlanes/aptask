package com.aptasks.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class LoginResponse(
    @JsonProperty("access_token")
    val accessToken: String,
    @JsonProperty("token_type")
    val tokenType: String,
    @JsonProperty("expires_in")
    val expiresIn: Int,
    @JsonProperty("refresh_token")
    val refreshToken: String,
    val user: UserInfo
)

data class UserInfo(
    val id: String,
    val email: String? = null,
    val role: String? = null,
    @JsonProperty("created_at")
    val createdAt: String? = null
)
