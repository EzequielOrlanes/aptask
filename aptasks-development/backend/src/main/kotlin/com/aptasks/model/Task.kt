package com.aptasks.model

import jakarta.persistence.*
import java.util.UUID

@Entity
@Table(name = "tasks")
data class Task(
    @Id
    val id: UUID = UUID.randomUUID(),
    val description: String = "",
    @Column(name="in_time")
    var inTime: Boolean = true,
    @Column(name = "user_id")
    var userId: String = ""
)