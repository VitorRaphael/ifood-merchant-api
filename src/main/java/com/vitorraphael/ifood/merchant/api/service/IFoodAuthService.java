package com.vitorraphael.ifood.merchant.api.service;

import com.vitorraphael.ifood.merchant.api.exception.IFoodApiException;
import com.vitorraphael.ifood.merchant.api.exception.TokenIndisponivelException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;

@Service
public class IFoodAuthService {

    @Value("${ifood.client.id}")
    private String clientId;

    @Value("${ifood.client.secret}")
    private String clientSecret;

    private static final String TOKEN_FILE = "tokens.json";
    private static final long MARGEM_SEGURANCA_SEGUNDOS = 60;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    // Protege a leitura/escrita de tokens.json: o polling de eventos (a cada 30s) e
    // requisições HTTP concorrentes podiam disparar autenticar() ao mesmo tempo e
    // corromper o arquivo com escritas entrelaçadas.
    private final Object tokenLock = new Object();

    public void autenticar() {
        synchronized (tokenLock) {
            String body = "grantType=client_credentials"
                    + "&clientId=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8)
                    + "&clientSecret=" + URLEncoder.encode(clientSecret, StandardCharsets.UTF_8);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://merchant-api.ifood.com.br/authentication/v1.0/oauth/token"))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            try {
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() != 200) {
                    throw new IFoodApiException(response.statusCode(), "iFood recusou a autenticação: " + response.body());
                }

                ObjectNode token = (ObjectNode) objectMapper.readTree(response.body());
                long expiresIn = token.get("expiresIn").asLong();
                Instant expiraEm = Instant.now().plusSeconds(expiresIn);
                token.put("expiraEm", expiraEm.toString());

                Files.writeString(Path.of(TOKEN_FILE), token.toString());
            } catch (IOException | InterruptedException e) {
                throw new IFoodApiException(0, "Falha de conexão ao autenticar com o iFood: " + e.getMessage(), e);
            }
        }
    }

    public String getValidToken() {
        synchronized (tokenLock) {
            if (tokenExpiradoOuInexistente()) {
                autenticar();
            }

            try {
                String content = Files.readString(Path.of(TOKEN_FILE));
                JsonNode token = objectMapper.readTree(content);
                return token.get("accessToken").asString();
            } catch (IOException e) {
                throw new TokenIndisponivelException("Não foi possível ler o token salvo: " + e.getMessage(), e);
            }
        }
    }

    private boolean tokenExpiradoOuInexistente() {
        try {
            String content = Files.readString(Path.of(TOKEN_FILE));
            JsonNode token = objectMapper.readTree(content);
            JsonNode expiraEmNode = token.get("expiraEm");

            if (expiraEmNode == null) {
                return true;
            }

            Instant expiraEm = Instant.parse(expiraEmNode.asString());
            return Instant.now().isAfter(expiraEm.minusSeconds(MARGEM_SEGURANCA_SEGUNDOS));
        } catch (IOException e) {
            return true;
        }
    }
}
