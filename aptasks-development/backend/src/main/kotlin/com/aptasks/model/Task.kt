package com.aptasks.model

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "tasks")
data class Task(
    @Id
    val id: UUID = UUID.randomUUID(),
    var description: String = "",
    @Column(name="in_time")
    var inTime: Boolean = true,
    @Column(name = "user_id")
    var userId: String = "",
    var isCompleted: Boolean = false,
    @Column(name = "due_at")
    var dueAt: Instant? = null,
    @Column(name = "fine_applied_at")
    var fineAppliedAt: Instant? = null,
    @Column(name = "decision_at")
    var decisionAt: Instant? = null
)
