package com.aptasks.dto

data class ApiResponse<T>(
    val success: Boolean,
    val message: String? = null,
    val data: T? = null,
    val count: Int? = null,
    val error: String? = null,
    val timestamp: String? = null
)
