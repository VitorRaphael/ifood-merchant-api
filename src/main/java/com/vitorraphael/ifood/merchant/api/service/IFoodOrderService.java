package com.vitorraphael.ifood.merchant.api.service;

import org.springframework.stereotype.Service;

@Service
public class IFoodOrderService {

    private static final String ORDER_URL = "https://merchant-api.ifood.com.br/order/v1.0/orders";

    private final IFoodAuthService authService;
    private final IFoodHttpClient httpClient;

    public IFoodOrderService(IFoodAuthService authService, IFoodHttpClient httpClient) {
        this.authService = authService;
        this.httpClient = httpClient;
    }

    public String buscarPedido(String orderId) {
        return httpClient.get(ORDER_URL + "/" + orderId, authService.getValidToken());
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
        // "{}" em vez de corpo vazio: o Akamai na frente da API do iFood devolvia
        // 411 Length Required pra POST com Content-Type: application/json e corpo
        // zero — um corpo mínimo de verdade evita essa rejeição de borda.
        httpClient.post(ORDER_URL + "/" + orderId + "/" + acao, authService.getValidToken(), "{}");
    }
}
