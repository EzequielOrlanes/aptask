package com.aptasks

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableJpaRepositories("com.aptasks.repository")
@EntityScan("com.aptasks.model")
@EnableScheduling
class AptasksApplication

fun main(args: Array<String>) {
    runApplication<AptasksApplication>(*args)
}
