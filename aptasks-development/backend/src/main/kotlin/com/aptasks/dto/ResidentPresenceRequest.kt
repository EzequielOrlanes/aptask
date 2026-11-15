package com.aptasks.dto

data class ResidentPresenceRequest(val isAtHome: Boolean)

data class ResidentResponse(
    val id: java.util.UUID,
    val name: String,
    val isAtHome: Boolean
)
