package com.aptasks.scheduler

import com.aptasks.service.KeepAliveService
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class KeepAliveScheduler(private val keepAliveService: KeepAliveService) {
    /** Pinga o próprio servidor a cada 2 minutos (padrão) para a hospedagem não hibernar. */
    @Scheduled(
        fixedRateString = "\${keepalive.interval-ms:120000}",
        initialDelayString = "\${keepalive.interval-ms:120000}",
    )
    fun ping() {
        keepAliveService.ping()
    }
}
