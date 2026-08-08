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
import java.util.Map;
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

    // Algumas APIs (ex.: Financial) exigem headers extras pra retornar os dados
    // fixos do ambiente de homologação em vez de dado real de produção.
    public String get(String url, String token, Map<String, String> headersExtras) {
        HttpRequest.Builder builder = requestBase(url, token).GET();
        headersExtras.forEach(builder::header);
        return enviar(builder);
    }

    public String post(String url, String token, String corpoJson) {
        return enviar(requestBase(url, token)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(corpoJson)));
    }

    // Mesmo motivo do get(url, token, headersExtras): o módulo Analytics também
    // exige x-request-homologation:true em ambiente de teste, só que num POST.
    public String post(String url, String token, String corpoJson, Map<String, String> headersExtras) {
        HttpRequest.Builder builder = requestBase(url, token)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(corpoJson));
        headersExtras.forEach(builder::header);
        return enviar(builder);
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

    // Critério de homologação (Authentication, "Considerações finais"): erro 5xx
    // da iFood merece nova tentativa com espera crescente, não desistência na
    // primeira falha — pode ser uma instabilidade passageira do lado deles.
    private static final int MAX_TENTATIVAS = 3;
    private static final long ESPERA_INICIAL_MS = 500;

    private String enviar(HttpRequest.Builder requestBuilder) {
        HttpRequest request = requestBuilder.build();
        long esperaMs = ESPERA_INICIAL_MS;

        for (int tentativa = 1; tentativa <= MAX_TENTATIVAS; tentativa++) {
            try {
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                boolean erroServidor = response.statusCode() >= 500;
                boolean aindaTemTentativa = tentativa < MAX_TENTATIVAS;
                if (erroServidor && aindaTemTentativa) {
                    log.warn("iFood respondeu {} (tentativa {}/{}) — nova tentativa em {}ms.",
                            response.statusCode(), tentativa, MAX_TENTATIVAS, esperaMs);
                    dormir(esperaMs);
                    esperaMs *= 2;
                    continue;
                }

                return tratarResposta(response);
            } catch (java.io.IOException e) {
                if (tentativa < MAX_TENTATIVAS) {
                    log.warn("Falha de conexão com o iFood (tentativa {}/{}): {} — nova tentativa em {}ms.",
                            tentativa, MAX_TENTATIVAS, e.getMessage(), esperaMs);
                    dormir(esperaMs);
                    esperaMs *= 2;
                    continue;
                }
                throw new IFoodApiException(0, "Falha de conexão com o iFood: " + e.getMessage(), e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IFoodApiException(0, "Falha de conexão com o iFood: " + e.getMessage(), e);
            }
        }

        // Inalcançável: o loop sempre retorna ou lança antes de sair por conta própria.
        throw new IllegalStateException("Loop de tentativas terminou sem resposta nem erro.");
    }

    private void dormir(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IFoodApiException(0, "Interrompido ao aguardar nova tentativa junto ao iFood.", e);
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
