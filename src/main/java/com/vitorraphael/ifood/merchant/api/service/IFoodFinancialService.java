package com.vitorraphael.ifood.merchant.api.service;

import com.vitorraphael.ifood.merchant.api.exception.IFoodApiException;
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
            throw new IFoodApiException(0, "Falha de conexão com o iFood: " + e.getMessage(), e);
        }
    }

    private String tratarResposta(HttpResponse<String> response) {
        int status = response.statusCode();

        if (status >= 200 && status < 300) {
            return response.body();
        }
        if (status == 401) {
            throw new IFoodApiException(status, "Token inválido ou expirado junto ao iFood (verifique também se o módulo Financial está liberado para essa loja).");
        }
        if (status == 403) {
            throw new IFoodApiException(status, "Sem permissão para acessar o financeiro dessa loja.");
        }
        if (status == 404) {
            throw new IFoodApiException(status, "Nenhuma liquidação encontrada para esse período: " + response.body());
        }
        if (status >= 500) {
            throw new IFoodApiException(status, "O iFood está com problemas no momento (status " + status + ").");
        }
        throw new IFoodApiException(status, "iFood recusou o pedido [" + status + "]: " + response.body());
    }
}