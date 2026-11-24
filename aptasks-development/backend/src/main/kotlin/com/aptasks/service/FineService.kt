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
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID

data class CancelledFines(val cancelled: Int, val keptPaid: Int)

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

    /**
     * Cancela a multa automática de uma tarefa. A multa não guarda a tarefa de origem, mas é criada
     * no mesmo instante em que a tarefa registra [appliedAt], então vale a janela de dois segundos.
     * Multas já pagas ficam e são contadas em [CancelledFines.keptPaid].
     */
    @Transactional
    fun cancelAutomaticFines(receiverId: UUID, appliedAt: Instant): CancelledFines =
        cancelFinesBetween(receiverId, appliedAt.minusSeconds(2), appliedAt.plusSeconds(2))

    /** Cancela as multas não pagas do morador criadas entre [from] e [to]; as pagas ficam. */
    @Transactional
    fun cancelFinesBetween(receiverId: UUID, from: Instant, to: Instant): CancelledFines {
        val zone = ZoneId.systemDefault()
        val start = LocalDateTime.ofInstant(from, zone)
        val end = LocalDateTime.ofInstant(to, zone)
        val keptPaid = fineRepository.countPaidBetween(receiverId, start, end)
        val cancelled = fineRepository.deleteUnpaidBetween(receiverId, start, end)
        return CancelledFines(cancelled, keptPaid)
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
