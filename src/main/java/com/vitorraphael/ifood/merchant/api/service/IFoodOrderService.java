package com.vitorraphael.ifood.merchant.api.service;

import com.vitorraphael.ifood.merchant.api.exception.IFoodApiException;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Service
public class IFoodOrderService {

    private static final String ORDER_URL = "https://merchant-api.ifood.com.br/order/v1.0/orders";

    private final IFoodAuthService authService;

    public IFoodOrderService(IFoodAuthService authService) {
        this.authService = authService;
    }

    public String buscarPedido(String orderId) {
        String token = authService.getValidToken();

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ORDER_URL + "/" + orderId))
                .header("Authorization", "Bearer " + token)
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return response.body();
            }
            throw new IFoodApiException(response.statusCode(), "iFood recusou o pedido [" + response.statusCode() + "]: " + response.body());
        } catch (java.io.IOException | InterruptedException e) {
            throw new IFoodApiException(0, "Falha de conexão com o iFood: " + e.getMessage(), e);
        }
    }

    public void confirmarPedido(String orderId) {
        executarAcao(orderId, "confirm");
    }

    public void marcarProntoParaColeta(String orderId) {
        executarAcao(orderId, "readyToPickup");
    }

    public void despacharPedido(String orderId) {
        executarAcao(orderId, "dispatch");
    }

    private void executarAcao(String orderId, String acao) {
        String token = authService.getValidToken();

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ORDER_URL + "/" + orderId + "/" + acao))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(10))
                // "{}" em vez de noBody(): o Akamai na frente da API do iFood devolvia
                // 411 Length Required pra POST com Content-Type: application/json e
                // corpo zero — um corpo mínimo de verdade evita essa rejeição de borda.
                .POST(HttpRequest.BodyPublishers.ofString("{}"))
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IFoodApiException(response.statusCode(),
                        "iFood recusou a ação [" + acao + "] no pedido " + orderId + " [" + response.statusCode() + "]: " + response.body());
            }
        } catch (java.io.IOException | InterruptedException e) {
            throw new IFoodApiException(0, "Falha de conexão com o iFood: " + e.getMessage(), e);
        }
    }
}