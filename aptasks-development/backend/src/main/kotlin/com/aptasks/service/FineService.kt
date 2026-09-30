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

        // Valida se as strings de UUID são válidas antes de prosseguir
        val senderUuid = runCatching { UUID.fromString(dto.sender) }.getOrNull()
            ?: throw IllegalArgumentException("UUID do remetente (sender) é inválido.")

        val receiverUuid = runCatching { UUID.fromString(dto.receiver) }.getOrNull()
            ?: throw IllegalArgumentException("UUID do destinatário (receiver) é inválido.")

        val fine = Fine(
            timestamp = dto.timestamp.toString(),
            amount = dto.amount.toFloat(),
            sender = senderUuid,
            receiver = receiverUuid,
            isPaid = false
        )

        fineRepository.save(fine)

    }
}