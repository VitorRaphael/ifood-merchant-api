package com.vitorraphael.ifood.merchant.api.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class IFoodMerchantService {

    private static final String BASE_URL = "https://merchant-api.ifood.com.br/merchant/v1.0";

    private final IFoodAuthService authService;
    private final IFoodHttpClient httpClient;

    @Value("${ifood.merchant.id}")
    private String merchantId;

    public IFoodMerchantService(IFoodAuthService authService, IFoodHttpClient httpClient) {
        this.authService = authService;
        this.httpClient = httpClient;
    }

    public String listarLojas() {
        return httpClient.get(BASE_URL + "/merchants", authService.getValidToken());
    }

    public String buscarStatus() {
        return httpClient.get(BASE_URL + "/merchants/" + merchantId + "/status", authService.getValidToken());
    }

    public String buscarDetalhes() {
        return httpClient.get(BASE_URL + "/merchants/" + merchantId, authService.getValidToken());
    }

    public String buscarHorarios() {
        return httpClient.get(BASE_URL + "/merchants/" + merchantId + "/opening-hours", authService.getValidToken());
    }

    public String atualizarHorarios(String corpoJson) {
        return httpClient.put(BASE_URL + "/merchants/" + merchantId + "/opening-hours", authService.getValidToken(), corpoJson);
    }

    public String buscarPausas() {
        return httpClient.get(BASE_URL + "/merchants/" + merchantId + "/interruptions", authService.getValidToken());
    }

    public String criarPausa(String corpoJson) {
        return httpClient.post(BASE_URL + "/merchants/" + merchantId + "/interruptions", authService.getValidToken(), corpoJson);
    }

    public String removerPausa(String idPausa) {
        return httpClient.delete(BASE_URL + "/merchants/" + merchantId + "/interruptions/" + idPausa, authService.getValidToken());
    }
}
