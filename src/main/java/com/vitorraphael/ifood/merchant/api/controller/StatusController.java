package com.vitorraphael.ifood.merchant.api.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@RestController
public class StatusController {

    @Value("${ifood.client.id}")
    private String clientId;

    @Value("${ifood.merchant.id}")
    private String merchantId;

    private static final DateTimeFormatter FORMATO_DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    @GetMapping("/api/status")
    public String status() {
        return "API no ar!";
    }

    // Usado só para exibir Client ID / Merchant ID / horário do servidor no
    // front-end durante a gravação dos vídeos de homologação do iFood — o
    // avaliador precisa ver esses dados na tela, não só no back-end.
    @GetMapping("/api/status/homologacao")
    public Map<String, String> homologacao() {
        return Map.of(
                "clientId", clientId,
                "merchantId", merchantId,
                "dataHoraServidor", LocalDateTime.now().format(FORMATO_DATA_HORA)
        );
    }
}