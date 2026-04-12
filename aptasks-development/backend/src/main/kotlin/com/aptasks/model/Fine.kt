package com.aptasks.model

import jakarta.persistence.*
import java.util.UUID

@Entity
@Table(name = "fine")
data class Fine(
    @Id
    val id: UUID = UUID.randomUUID(),
    var timestamp: String ,
    var amount: Float,
    var sender: UUID,
    var receiver: UUID,
    @Column(name = "is_paid")
    var isPaid: Boolean = false
)