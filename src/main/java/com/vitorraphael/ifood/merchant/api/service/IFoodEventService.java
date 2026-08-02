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
import java.util.ArrayList;
import java.util.List;

@Service
public class IFoodEventService {

    private static final Logger log = LoggerFactory.getLogger(IFoodEventService.class);
    private static final String EVENTS_URL = "https://merchant-api.ifood.com.br/events/v1.0/events:polling";
    private static final String ACK_URL = "https://merchant-api.ifood.com.br/events/v1.0/events/acknowledgment";

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
        String token;
        try {
            token = authService.getValidToken();
        } catch (RuntimeException e) {
            log.warn("Polling de eventos pulado: não foi possível obter um token válido ({}).", e.getMessage());
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
                processarEventos(response.body(), token);
                return;
            }
            log.error("Polling de eventos falhou [{}]: {}", response.statusCode(), response.body());
        } catch (java.io.IOException | InterruptedException e) {
            log.error("Falha de conexão no polling de eventos: {}", e.getMessage());
        }
    }

    private void processarEventos(String corpoJson, String token) {
        JsonNode eventos = objectMapper.readTree(corpoJson);
        List<String> idsEventos = new ArrayList<>();

        for (JsonNode evento : eventos) {
            JsonNode idNode = evento.get("id");
            if (idNode != null) {
                idsEventos.add(idNode.asString());
            }

            JsonNode codigoNode = evento.get("code");
            JsonNode orderIdNode = evento.get("orderId");

            String codigo = codigoNode != null ? codigoNode.asString() : "";
            String orderId = orderIdNode != null ? orderIdNode.asString() : null;

            if (orderId == null) {
                continue;
            }

            try {
                switch (codigo) {
                    case "PLC" -> {
                        try {
                            orderService.confirmarPedido(orderId);
                            log.info("Pedido {} confirmado automaticamente junto ao iFood.", orderId);
                        } catch (Exception e) {
                            log.warn("Falha ao auto-confirmar o pedido {} (seguindo mesmo assim): {}", orderId, e.getMessage());
                        }
                        String pedidoJson = orderService.buscarPedido(orderId);
                        vendaService.processarPedido(pedidoJson);
                        log.info("Venda persistida a partir do evento: {}", orderId);
                    }
                    case "CON" -> {
                        vendaService.atualizarStatus(orderId, "CONCLUIDO");
                        log.info("Venda concluída: {}", orderId);
                    }
                    case "CAN" -> {
                        vendaService.atualizarStatus(orderId, "CANCELADO");
                        log.info("Venda cancelada: {}", orderId);
                    }
                    default -> log.info("Evento ignorado (código {}): {}", codigo, orderId);
                }
            } catch (Exception e) {
                log.error("Falha ao processar evento {} do pedido {}: {}", codigo, orderId, e.getMessage());
            }
        }

        confirmarEventos(idsEventos, token);
    }

    private void confirmarEventos(List<String> idsEventos, String token) {
        if (idsEventos.isEmpty()) {
            return;
        }

        StringBuilder corpo = new StringBuilder("[");
        for (int i = 0; i < idsEventos.size(); i++) {
            if (i > 0) {
                corpo.append(",");
            }
            corpo.append("{\"id\":\"").append(idsEventos.get(i)).append("\"}");
        }
        corpo.append("]");

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ACK_URL))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(10))
                .POST(HttpRequest.BodyPublishers.ofString(corpo.toString()))
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                log.info("Eventos reconhecidos junto ao iFood: {}", idsEventos.size());
            } else {
                log.error("Falha ao reconhecer eventos [{}]: {}", response.statusCode(), response.body());
            }
        } catch (java.io.IOException | InterruptedException e) {
            log.error("Falha de conexão ao reconhecer eventos: {}", e.getMessage());
        }
    }
}