package com.vitorraphael.ifood.merchant.api.service;

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
        if (token == null) {
            throw new IllegalStateException("Sem token válido. Chame /api/auth/autenticar primeiro.");
        }

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
            throw new RuntimeException("iFood recusou o pedido [" + response.statusCode() + "]: " + response.body());
        } catch (java.io.IOException | InterruptedException e) {
            throw new RuntimeException("Falha de conexão com o iFood: " + e.getMessage(), e);
        }
    }
}