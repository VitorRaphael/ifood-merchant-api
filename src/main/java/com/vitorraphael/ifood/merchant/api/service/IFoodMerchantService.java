package com.vitorraphael.ifood.merchant.api.service;

import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;

@Service
public class IFoodMerchantService {

    private static final String BASE_URL = "https://merchant-api.ifood.com.br/merchant/v1.0";

    private final IFoodAuthService authService;

    public IFoodMerchantService(IFoodAuthService authService) {
        this.authService = authService;
    }

    public String listarLojas() {
        return executarGet(BASE_URL + "/merchants");
    }

    private String executarGet(String url) {
        String token = authService.getValidToken();
        if (token == null) {
            throw new IllegalStateException("Sem token válido. Chame /api/auth/autenticar primeiro.");
        }

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + token)
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(10))
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

        if (status == 200) {
            return response.body();
        }
        if (status == 401) {
            throw new RuntimeException("Token inválido ou expirado. Autentique novamente.");
        }
        if (status == 403) {
            throw new RuntimeException("Sem permissão para acessar essa loja.");
        }
        if (status == 429) {
            String retryAfter = Optional.ofNullable(response.headers().firstValue("Retry-After").orElse(null))
                    .orElse("alguns segundos");
            throw new RuntimeException("Limite de requisições atingido. Tente novamente em " + retryAfter + ".");
        }
        if (status >= 500) {
            throw new RuntimeException("O iFood está com problemas no momento (status " + status + ").");
        }
        throw new RuntimeException("iFood recusou o pedido [" + status + "]: " + response.body());
    }
}