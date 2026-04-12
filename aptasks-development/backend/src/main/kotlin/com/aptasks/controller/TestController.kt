package com.aptasks.controller

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1") // Prefixo da sua rota
class TestController {

    @GetMapping("/hello")
    fun sayHello(): Map<String, String> {
        // Retornamos um Map para que o Spring transforme automaticamente em JSON
        return mapOf("message" to "Conexão com a porta 3000 funcionando!", "status" to "OK")
    }
}