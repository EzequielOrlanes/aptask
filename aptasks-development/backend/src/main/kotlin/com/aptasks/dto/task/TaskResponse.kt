package com.aptasks.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class TaskResponse(
    val description: String,
    val expired_at: String,
    val is_done: Boolean
)