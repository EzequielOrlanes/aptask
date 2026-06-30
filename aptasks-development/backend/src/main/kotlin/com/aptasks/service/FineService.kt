package com.aptasks.service

import com.aptasks.dto.fine.FineRequest
import com.aptasks.model.Fine
import com.aptasks.repository.FineRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class FineService(private val fineRepository: FineRepository) {

    @Transactional
    fun applyFine(dto: FineRequest) {
        // Mapeando o DTO para o formato exato que o seu Model Fine espera
        val fine = Fine(
            timestamp = dto.timestamp.toString(),             // Converte LocalDateTime para String
            amount = dto.amount.toFloat(),                    // Converte Double para Float
            sender = UUID.fromString(dto.sender),             // Converte String para UUID
            receiver = UUID.fromString(dto.receiver),         // Converte String para UUID
            isPaid = false                                    // Valor padrão inicial
        )

        fineRepository.save(fine)
    }
}