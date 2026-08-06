package com.vitorraphael.ifood.merchant.api.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class IFoodFinancialService {

    private static final Logger log = LoggerFactory.getLogger(IFoodFinancialService.class);
    private static final String SETTLEMENTS_URL = "https://merchant-api.ifood.com.br/financial/v3.0/merchants/";

    private final IFoodAuthService authService;
    private final IFoodHttpClient httpClient;

    @Value("${ifood.merchant.id}")
    private String merchantId;

    // O iFood exige esse header nas chamadas de Sales/Events/Reconciliation/
    // Settlements/Antecipation pra devolver os dados fixos do ambiente de
    // homologação (loja de teste). Deve virar `false` quando o app apontar
    // pra credenciais/loja de produção.
    @Value("${ifood.financeiro.homologacao:true}")
    private boolean modoHomologacao;

    public IFoodFinancialService(IFoodAuthService authService, IFoodHttpClient httpClient) {
        this.authService = authService;
        this.httpClient = httpClient;
    }

    public String buscarLiquidacoes(String dataInicio, String dataFim) {
        String url = SETTLEMENTS_URL + merchantId + "/settlements?beginPaymentDate=" + dataInicio + "&endPaymentDate=" + dataFim;
        Map<String, String> headers = modoHomologacao
                ? Map.of("x-request-homologation", "true")
                : Map.of();
        String resposta = httpClient.get(url, authService.getValidToken(), headers);
        // TEMPORÁRIO — diagnóstico da lista vazia no painel. Remover depois de confirmar
        // o formato real da resposta do settlements no ambiente de homologação.
        log.info("Resposta bruta do settlements [{} até {}]: {}", dataInicio, dataFim, resposta);
        return resposta;
    }
}
