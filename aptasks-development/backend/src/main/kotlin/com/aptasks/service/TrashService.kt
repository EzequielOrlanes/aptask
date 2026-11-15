package com.aptasks.service

import com.aptasks.dto.ResidentResponse
import com.aptasks.dto.trash.TrashStatusResponse
import com.aptasks.exception.TrashConflictException
import com.aptasks.exception.TrashForbiddenException
import com.aptasks.model.User
import com.aptasks.repository.TrashRepository
import com.aptasks.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

/** Regras da lixeira: quem é o responsável, o nível de enchimento (vazio, meio cheio, cheio) e os esvaziamentos. */
@Service
class TrashService(
    private val trashRepository: TrashRepository,
    private val userRepository: UserRepository
) {
    fun getTodayStatus(): TrashStatusResponse {
        val status = trashRepository.findStatus()
        return TrashStatusResponse(
            responsible = status.responsibleUserId?.let { findResident(it) },
            fillLevel = status.fillLevel,
            isFull = status.isFull,
            reportedAt = status.reportedAt,
            reportedBy = status.reportedByUserId?.let { findResident(it) }
        )
    }

    @Transactional
    fun assignResponsible(residentId: String?, now: Instant = Instant.now()): TrashStatusResponse {
        val resident = requireResident(residentId)
        trashRepository.ensureRow()
        trashRepository.setResponsible(resident.id, now)
        trashRepository.insertEvent("RESPONSIBLE_CHANGED", resident.id, now)
        return getTodayStatus()
    }

    @Transactional
    fun reportHalf(reportedByResidentId: String?, now: Instant = Instant.now()): TrashStatusResponse {
        val resident = requireResident(reportedByResidentId)
        // Já há aviso de meio cheio ou cheio: responde com o estado atual e não grava nada.
        if (trashRepository.findStatus().fillLevel != "EMPTY") {
            return getTodayStatus()
        }
        trashRepository.ensureRow()
        if (!trashRepository.markHalfIfEmpty(resident.id, now)) {
            return getTodayStatus()
        }
        trashRepository.insertEvent("REPORTED_HALF", resident.id, now)
        return getTodayStatus()
    }

    @Transactional
    fun reportFull(reportedByResidentId: String?, now: Instant = Instant.now()): TrashStatusResponse {
        val resident = requireResident(reportedByResidentId)
        // Aviso repetido: responde com o estado atual e não registra nada no banco.
        if (trashRepository.findStatus().isFull) {
            return getTodayStatus()
        }
        trashRepository.ensureRow()
        if (!trashRepository.markFullIfNotFull(resident.id, now)) {
            return getTodayStatus()
        }
        trashRepository.insertEvent("REPORTED_FULL", resident.id, now)
        return getTodayStatus()
    }

    @Transactional
    fun markEmptied(updatedByResidentId: String?, now: Instant = Instant.now()): TrashStatusResponse {
        val resident = requireResident(updatedByResidentId)
        val current = trashRepository.findStatus()
        // Só o responsável atual pode dizer que o lixo foi retirado.
        if (current.responsibleUserId != resident.id) {
            throw TrashForbiddenException("Só o responsável pelo lixo pode marcar como esvaziado.")
        }
        if (current.fillLevel == "EMPTY") {
            throw TrashConflictException("O lixo já está vazio.")
        }
        trashRepository.ensureRow()
        if (!trashRepository.markEmptiedIfNotEmpty(now)) {
            throw TrashConflictException("O lixo já está vazio.")
        }
        trashRepository.insertEvent("MARKED_EMPTIED", resident.id, now)
        return getTodayStatus()
    }

    private fun findResident(id: UUID): ResidentResponse? =
        userRepository.findById(id)?.toResponse()

    private fun requireResident(raw: String?): User {
        if (raw.isNullOrBlank()) {
            throw IllegalArgumentException("O identificador do morador é obrigatório.")
        }
        val id = runCatching { UUID.fromString(raw.trim()) }.getOrNull()
            ?: throw IllegalArgumentException("O identificador do morador é inválido.")
        return userRepository.findById(id)
            ?: throw IllegalArgumentException("Morador não encontrado.")
    }

    private fun User.toResponse() = ResidentResponse(id, name, isAtHome)
}
