package com.vitorraphael.ifood.merchant.api.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Service
public class IFoodFinancialService {

    private static final String SETTLEMENTS_URL = "https://merchant-api.ifood.com.br/financial/v3.0/merchants/";
    private final IFoodAuthService authService;

    @Value("${ifood.merchant.id}")
    private String merchantId;

    public IFoodFinancialService(IFoodAuthService authService) {
        this.authService = authService;
    }

    public String buscarLiquidacoes(String dataInicio, String dataFim) {
        String token = authService.getValidToken();
        if (token == null) {
            throw new IllegalStateException("Sem token válido. Chame /api/auth/autenticar primeiro.");
        }

        String url = SETTLEMENTS_URL + merchantId + "/settlements?beginPaymentDate=" + dataInicio + "&endPaymentDate=" + dataFim;

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + token)
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return tratarResposta(response);
        } catch (java.io.IOException | InterruptedException e) {
            throw new RuntimeException("Falha de conexão com o iFood: " + e.getMessage(), e);
        }
    }

    private String tratarResposta(HttpResponse<String> response) {
        int status = response.statusCode();

        if (status >= 200 && status < 300) {
            return response.body();
        }
        if (status == 401) {
            throw new RuntimeException("Token inválido ou expirado. Autentique novamente.");
        }
        if (status == 403) {
            throw new RuntimeException("Sem permissão para acessar o financeiro dessa loja.");
        }
        if (status == 404) {
            throw new RuntimeException("Nenhuma liquidação encontrada para esse período: " + response.body());
        }
        if (status >= 500) {
            throw new RuntimeException("O iFood está com problemas no momento (status " + status + ").");
        }
        throw new RuntimeException("iFood recusou o pedido [" + status + "]: " + response.body());
    }
}