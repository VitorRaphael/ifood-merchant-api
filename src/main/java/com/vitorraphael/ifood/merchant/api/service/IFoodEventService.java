package com.vitorraphael.ifood.merchant.api.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

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
    private final IFoodOrderService orderService;
    private final VendaService vendaService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public IFoodEventService(IFoodAuthService authService, IFoodOrderService orderService, VendaService vendaService) {
        this.authService = authService;
        this.orderService = orderService;
        this.vendaService = vendaService;
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
                processarEventos(response.body());
                return;
            }
            log.error("Polling de eventos falhou [{}]: {}", response.statusCode(), response.body());
        } catch (java.io.IOException | InterruptedException e) {
            log.error("Falha de conexão no polling de eventos: {}", e.getMessage());
        }
    }

    private void processarEventos(String corpoJson) {
        JsonNode eventos = objectMapper.readTree(corpoJson);

        for (JsonNode evento : eventos) {
            JsonNode codigoNode = evento.get("code");
            JsonNode orderIdNode = evento.get("orderId");

            String codigo = codigoNode != null ? codigoNode.asString() : "";
            String orderId = orderIdNode != null ? orderIdNode.asString() : null;

            if (!"PLC".equals(codigo) || orderId == null) {
                continue;
            }

            try {
                String pedidoJson = orderService.buscarPedido(orderId);
                vendaService.processarPedido(pedidoJson);
                log.info("Venda persistida automaticamente a partir do evento: {}", orderId);
            } catch (Exception e) {
                log.error("Falha ao processar evento do pedido {}: {}", orderId, e.getMessage());
            }
        }
    }
}