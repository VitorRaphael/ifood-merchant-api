package com.vitorraphael.ifood.merchant.api.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class IFoodFinancialService {

    private static final String SETTLEMENTS_URL = "https://merchant-api.ifood.com.br/financial/v3.0/merchants/";

    private final IFoodAuthService authService;
    private final IFoodHttpClient httpClient;

    @Value("${ifood.merchant.id}")
    private String merchantId;

    public IFoodFinancialService(IFoodAuthService authService, IFoodHttpClient httpClient) {
        this.authService = authService;
        this.httpClient = httpClient;
    }

    public String buscarLiquidacoes(String dataInicio, String dataFim) {
        String url = SETTLEMENTS_URL + merchantId + "/settlements?beginPaymentDate=" + dataInicio + "&endPaymentDate=" + dataFim;
        return httpClient.get(url, authService.getValidToken());
    }
}
