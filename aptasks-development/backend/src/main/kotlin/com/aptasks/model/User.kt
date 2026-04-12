package com.aptasks.model

import jakarta.persistence.*
import java.util.UUID

@Entity
@Table(name = "users")
data class User(
    @Id
    val id: UUID,
    val name: String,
    val email: String,
    @Column(name = "is_at_home")
    var isAtHome: Boolean = false,
    @Column(name = "homework_is_done")
    var homeWorkIsDone: Boolean = false
)