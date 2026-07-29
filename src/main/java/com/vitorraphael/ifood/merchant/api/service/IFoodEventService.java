package com.vitorraphael.ifood.merchant.api.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Service
public class IFoodEventService {

    private static final Logger log = LoggerFactory.getLogger(IFoodEventService.class);
    private static final String EVENTS_URL = "https://merchant-api.ifood.com.br/events/v1.0/events:polling";

    private final IFoodAuthService authService;

    public IFoodEventService(IFoodAuthService authService) {
        this.authService = authService;
    }

    @Scheduled(fixedRate = 30000)
    public void buscarEventos() {
        String token = authService.getValidToken();
        if (token == null) {
            log.warn("Polling de eventos pulado: sem token válido.");
            return;
        }

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(EVENTS_URL))
                .header("Authorization", "Bearer " + token)
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 204) {
                log.info("Polling de eventos: nenhum evento novo.");
                return;
            }
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                log.info("Polling de eventos: {}", response.body());
                return;
            }
            log.error("Polling de eventos falhou [{}]: {}", response.statusCode(), response.body());
        } catch (java.io.IOException | InterruptedException e) {
            log.error("Falha de conexão no polling de eventos: {}", e.getMessage());
        }
    }
}