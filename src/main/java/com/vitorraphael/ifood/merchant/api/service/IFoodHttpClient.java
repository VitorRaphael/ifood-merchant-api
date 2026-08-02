package com.vitorraphael.ifood.merchant.api.service;

import com.vitorraphael.ifood.merchant.api.exception.IFoodApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;

@Component
public class IFoodHttpClient {

    private static final Logger log = LoggerFactory.getLogger(IFoodHttpClient.class);

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public String get(String url, String token) {
        return enviar(requestBase(url, token).GET());
    }

    public String post(String url, String token, String corpoJson) {
        return enviar(requestBase(url, token)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(corpoJson)));
    }

    public String put(String url, String token, String corpoJson) {
        return enviar(requestBase(url, token)
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(corpoJson)));
    }

    public String delete(String url, String token) {
        return enviar(requestBase(url, token).DELETE());
    }

    private HttpRequest.Builder requestBase(String url, String token) {
        return HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + token)
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(10));
    }

    private String enviar(HttpRequest.Builder requestBuilder) {
        try {
            HttpResponse<String> response = client.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
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
            throw new IFoodApiException(status, "Token inválido ou expirado junto ao iFood.");
        }
        if (status == 403) {
            throw new IFoodApiException(status, "Sem permissão para essa operação junto ao iFood.");
        }
        if (status == 404) {
            throw new IFoodApiException(status, "Recurso não encontrado junto ao iFood.");
        }
        if (status == 409) {
            throw new IFoodApiException(status, "Conflito: essa operação esbarra em algo que já existe.");
        }
        if (status == 429) {
            String retryAfter = Optional.ofNullable(response.headers().firstValue("Retry-After").orElse(null))
                    .orElse("alguns segundos");
            throw new IFoodApiException(status, "Limite de requisições atingido. Tente novamente em " + retryAfter + ".");
        }
        if (status >= 500) {
            throw new IFoodApiException(status, "O iFood está com problemas no momento (status " + status + ").");
        }

        // O corpo cru da resposta só vai pro log, nunca pra mensagem devolvida ao cliente da
        // nossa própria API — evita vazar detalhes internos da integração pra quem chamar a gente.
        log.warn("iFood recusou a operação [{}]: {}", status, response.body());
        throw new IFoodApiException(status, "iFood recusou a operação [" + status + "].");
    }
}
