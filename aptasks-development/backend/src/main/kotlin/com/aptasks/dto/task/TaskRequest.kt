package com.aptasks.dto

import com.fasterxml.jackson.annotation.JsonProperty
import java.util.UUID

data class TaskRequest(
    var responsible_user: String
)

data class CreateTaskRequest(
    var responsible_user: UUID,
    var description: String
)