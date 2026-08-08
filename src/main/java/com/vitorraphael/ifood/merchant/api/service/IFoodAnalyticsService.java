package com.vitorraphael.ifood.merchant.api.service;

import com.vitorraphael.ifood.merchant.api.model.AnaliticaKpis;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class IFoodAnalyticsService {

    private static final String KPIS_URL = "https://merchant-api.ifood.com.br/analytics/v1.0/merchants/";

    private final IFoodAuthService authService;
    private final IFoodHttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${ifood.merchant.id}")
    private String merchantId;

    // Mesmo motivo do IFoodFinancialService: em homologação o iFood só devolve
    // o payload fixo do ambiente de teste com esse header. Vira `false` junto
    // com a troca para credenciais/loja de produção.
    @Value("${ifood.financeiro.homologacao:true}")
    private boolean modoHomologacao;

    public IFoodAnalyticsService(IFoodAuthService authService, IFoodHttpClient httpClient) {
        this.authService = authService;
        this.httpClient = httpClient;
    }

    public AnaliticaKpis buscarKpis(LocalDate inicio, LocalDate fim) {
        String corpo = montarCorpoRequisicao(inicio, fim);
        Map<String, String> headers = modoHomologacao
                ? Map.of("x-request-homologation", "true")
                : Map.of();

        String url = KPIS_URL + merchantId + "/orders/kpis";
        String resposta = httpClient.post(url, authService.getValidToken(), corpo, headers);
        return interpretarResposta(resposta, inicio, fim);
    }

    // Critério de homologação do Analytics: filter.referenceDate é obrigatório em
    // toda chamada, e é preciso mandar ao menos uma agregação (aqui: metrics +
    // terms). Sem groupBy de propósito -- metrics e terms sozinhos já devolvem os
    // totais e as distribuições numa única linha, que é o que o painel precisa
    // pra montar os cartões de KPI (a própria doc do iFood ensina a calcular
    // ticket médio assim: gmv.sum / orderStatus.value.CONCLUDED).
    private String montarCorpoRequisicao(LocalDate inicio, LocalDate fim) {
        ObjectNode referenceDate = objectMapper.createObjectNode();
        referenceDate.put("gte", inicio + " 00:00:00");
        referenceDate.put("lte", fim + " 23:59:59");

        ObjectNode filter = objectMapper.createObjectNode();
        filter.set("referenceDate", referenceDate);

        ObjectNode metrics = objectMapper.createObjectNode();
        metrics.putArray("gmv").add("sum").add("avg");
        metrics.putArray("gmvWithoutDelivery").add("sum");

        ObjectNode terms = objectMapper.createObjectNode();
        terms.putArray("salesChannel").add("count");
        terms.putArray("orderStatus").add("count");
        terms.putArray("paymentMethod").add("count");
        terms.putArray("deliveredBy").add("count");

        ObjectNode agg = objectMapper.createObjectNode();
        agg.set("metrics", metrics);
        agg.set("terms", terms);

        ObjectNode raiz = objectMapper.createObjectNode();
        raiz.set("filter", filter);
        raiz.set("agg", agg);
        raiz.put("page", 1);
        raiz.put("size", 20);

        return raiz.toString();
    }

    private AnaliticaKpis interpretarResposta(String respostaJson, LocalDate inicio, LocalDate fim) {
        JsonNode raiz = objectMapper.readTree(respostaJson);
        JsonNode dados = raiz.get("data");
        // Ambiente de teste pode devolver "data" vazio se a loja de teste não
        // tiver pedidos recentes o bastante no período pedido -- não é erro
        // nosso, é a mesma situação já vista no módulo Financial.
        JsonNode linha = (dados != null && dados.isArray() && !dados.isEmpty()) ? dados.get(0) : null;

        Map<String, Integer> porStatus = distribuicao(linha, "orderStatus");
        double gmvTotal = metrica(linha, "gmv", "sum");
        int pedidosConcluidos = porStatus.getOrDefault("CONCLUDED", 0);
        int pedidosCancelados = porStatus.getOrDefault("CANCELLED", 0);
        double ticketMedio = pedidosConcluidos > 0 ? gmvTotal / pedidosConcluidos : 0;

        return new AnaliticaKpis(
                inicio,
                fim,
                gmvTotal,
                metrica(linha, "gmvWithoutDelivery", "sum"),
                ticketMedio,
                pedidosConcluidos,
                pedidosCancelados,
                distribuicao(linha, "salesChannel"),
                porStatus,
                distribuicao(linha, "paymentMethod"),
                distribuicao(linha, "deliveredBy")
        );
    }

    private double metrica(JsonNode linha, String campo, String agregacao) {
        if (linha == null) {
            return 0;
        }
        JsonNode metricaNode = linha.get(campo);
        JsonNode valor = metricaNode != null ? metricaNode.get(agregacao) : null;
        return valor != null && !valor.isNull() ? valor.asDouble() : 0;
    }

    private Map<String, Integer> distribuicao(JsonNode linha, String campo) {
        Map<String, Integer> resultado = new LinkedHashMap<>();
        if (linha == null) {
            return resultado;
        }

        JsonNode termoNode = linha.get(campo);
        JsonNode valorNode = termoNode != null ? termoNode.get("value") : null;
        if (valorNode == null) {
            return resultado;
        }

        for (Map.Entry<String, JsonNode> entrada : valorNode.properties()) {
            resultado.put(entrada.getKey(), entrada.getValue().asInt());
        }
        return resultado;
    }
}
