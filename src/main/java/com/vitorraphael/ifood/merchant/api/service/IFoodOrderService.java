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

    // Não existe ação de API pra "concluir" um pedido diretamente — o evento
    // CONCLUDED só é gerado pela própria iFood, seja por timeout automático,
    // seja (aqui) pela validação do código de entrega passado ao cliente.
    public void confirmarEntrega(String orderId, String codigo) {
        if (codigo == null || !codigo.matches("[0-9]{1,8}")) {
            throw new IllegalArgumentException("Código de entrega inválido: deve conter só dígitos.");
        }
        String corpo = "{\"code\":\"" + codigo + "\"}";
        httpClient.post(ORDER_URL + "/" + orderId + "/verifyDeliveryCode", authService.getValidToken(), corpo);
    }

    private void executarAcao(String orderId, String acao) {
        // "{}" em vez de corpo vazio: o Akamai na frente da API do iFood devolvia
        // 411 Length Required pra POST com Content-Type: application/json e corpo
        // zero — um corpo mínimo de verdade evita essa rejeição de borda.
        httpClient.post(ORDER_URL + "/" + orderId + "/" + acao, authService.getValidToken(), "{}");
    }
}
