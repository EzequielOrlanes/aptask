package com.aptasks.dto

import com.fasterxml.jackson.annotation.JsonProperty
import java.util.UUID
import java.time.Instant

data class TaskRequest(
    var responsible_user: String
)

data class CreateTaskRequest(
    var responsible_user: UUID,
    var description: String,
    var dueAt: Instant? = null
)

data class UpdateTaskRequest(
    var responsible_user: UUID,
    var description: String,
    var dueAt: Instant? = null
)

data class TaskDecisionRequest(
    val done: Boolean
)
