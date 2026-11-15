package com.aptasks.service

import com.aptasks.dto.fine.FineRequest
import com.aptasks.dto.fine.FineResponse
import com.aptasks.exception.FineNotFoundException
import com.aptasks.model.Fine
import com.aptasks.repository.FineRepository
import com.aptasks.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID

@Service
class FineService(
    private val fineRepository: FineRepository,
    private val userRepository: UserRepository
) {
    companion object {
        /** Limite de NUMERIC(10, 2). */
        private val MAX_AMOUNT = BigDecimal("99999999.99")
    }

    @Transactional
    fun applyFine(dto: FineRequest): Fine {
        // Valida se as strings de UUID são válidas antes de prosseguir
        val senderUuid = runCatching { UUID.fromString(dto.sender) }.getOrNull()
            ?: throw IllegalArgumentException("UUID do remetente (sender) é inválido.")

        val receiverUuid = runCatching { UUID.fromString(dto.receiver) }.getOrNull()
            ?: throw IllegalArgumentException("UUID do destinatário (receiver) é inválido.")

        require(senderUuid != receiverUuid) { "Remetente e destinatário da multa precisam ser moradores diferentes." }

        val amount = validateAmount(dto.amount)

        require(userRepository.findById(senderUuid) != null) { "O remetente da multa não foi encontrado." }
        require(userRepository.findById(receiverUuid) != null) { "O destinatário da multa não foi encontrado." }

        val fine = Fine(
            timestamp = dto.timestamp,
            amount = amount,
            sender = senderUuid,
            receiver = receiverUuid,
            isPaid = false
        )
        return fineRepository.insert(fine)
    }

    /** Multas enviadas e recebidas pelo morador. */
    fun findForResident(userId: UUID): List<FineResponse> {
        require(userRepository.findById(userId) != null) { "O morador não foi encontrado." }
        return fineRepository.findByResident(userId)
    }

    /** Todas as multas (área administrativa). */
    fun findAll(): List<FineResponse> = fineRepository.findAll()

    /** Remove uma multa. Lança [FineNotFoundException] se ela não existe. */
    @Transactional
    fun deleteFine(id: UUID) {
        if (!fineRepository.deleteById(id)) throw FineNotFoundException(id)
    }

    /** Marca a multa como paga. Chamar de novo em uma multa já paga não tem efeito. */
    @Transactional
    fun markPaid(id: UUID): Fine {
        val fine = fineRepository.findById(id) ?: throw FineNotFoundException(id)
        if (fine.isPaid) return fine
        fineRepository.markPaid(id)
        return fine.copy(isPaid = true)
    }

    private fun validateAmount(amount: Double): BigDecimal {
        require(amount.isFinite()) { "O valor da multa precisa ser um número válido." }
        val value = BigDecimal.valueOf(amount).setScale(2, RoundingMode.HALF_UP)
        require(value > BigDecimal.ZERO) { "O valor da multa precisa ser maior que zero." }
        require(value <= MAX_AMOUNT) { "O valor da multa é alto demais." }
        return value
    }
}
