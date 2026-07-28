package com.vitorraphael.ifood.merchant.api.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class IFoodAuthService {

    @Value("${ifood.client.id}")
    private String clientId;

    @Value("${ifood.client.secret}")
    private String clientSecret;

    private static final String TOKEN_FILE = "tokens.json";

    public void autenticar() {
        HttpClient client = HttpClient.newHttpClient();

        String body = "grantType=client_credentials&clientId=" + clientId + "&clientSecret=" + clientSecret;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://merchant-api.ifood.com.br/authentication/v1.0/oauth/token"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new RuntimeException("iFood recusou a autenticação [" + response.statusCode() + "]: " + response.body());
            }

            Files.writeString(Path.of(TOKEN_FILE), response.body());
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Falha ao autenticar com o iFood", e);
        }
    }

    public String getValidToken() {
        try {
            String content = Files.readString(Path.of(TOKEN_FILE));
            return content.split("\"accessToken\":\"")[1].split("\"")[0];
        } catch (IOException e) {
            return null;
        }
    }
}