package com.aptasks.controller

import com.aptasks.dto.ApiResponse
import com.aptasks.dto.trash.TrashEmptyRequest
import com.aptasks.dto.trash.TrashReportRequest
import com.aptasks.dto.trash.TrashResponsibleRequest
import com.aptasks.dto.trash.TrashStatusResponse
import com.aptasks.service.TrashService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/trash")
class TrashController(private val trashService: TrashService) {

    @GetMapping("/today")
    fun getToday(): ResponseEntity<ApiResponse<TrashStatusResponse>> =
        ResponseEntity.ok(
            ApiResponse(success = true, message = "Status do lixo carregado.", data = trashService.getTodayStatus())
        )

    @PatchMapping("/responsible")
    fun assignResponsible(
        @RequestBody request: TrashResponsibleRequest
    ): ResponseEntity<ApiResponse<TrashStatusResponse>> =
        ResponseEntity.ok(
            ApiResponse(
                success = true,
                message = "Responsável pelo lixo atualizado.",
                data = trashService.assignResponsible(request.residentId)
            )
        )

    @PostMapping("/half")
    fun reportHalf(@RequestBody request: TrashReportRequest): ResponseEntity<ApiResponse<TrashStatusResponse>> =
        ResponseEntity.ok(
            ApiResponse(
                success = true,
                message = "Aviso de lixo meio cheio registrado.",
                data = trashService.reportHalf(request.reportedByResidentId)
            )
        )

    @PostMapping("/full")
    fun reportFull(@RequestBody request: TrashReportRequest): ResponseEntity<ApiResponse<TrashStatusResponse>> =
        ResponseEntity.ok(
            ApiResponse(
                success = true,
                message = "Aviso de lixo cheio registrado.",
                data = trashService.reportFull(request.reportedByResidentId)
            )
        )

    @PostMapping("/empty")
    fun markEmptied(@RequestBody request: TrashEmptyRequest): ResponseEntity<ApiResponse<TrashStatusResponse>> =
        ResponseEntity.ok(
            ApiResponse(
                success = true,
                message = "Lixo marcado como esvaziado.",
                data = trashService.markEmptied(request.updatedByResidentId)
            )
        )
}
