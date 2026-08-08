package com.vitorraphael.ifood.merchant.api.model;

import java.time.LocalDate;
import java.util.Map;

// Não é uma entidade JPA: vem sempre ao vivo da API Analytics do iFood
// (dados agregados D-1, nunca em tempo real), não faz sentido persistir.
public record AnaliticaKpis(
        LocalDate periodoInicio,
        LocalDate periodoFim,
        double gmvTotal,
        double gmvSemEntregaTotal,
        double ticketMedio,
        int pedidosConcluidos,
        int pedidosCancelados,
        Map<String, Integer> porCanal,
        Map<String, Integer> porStatus,
        Map<String, Integer> porPagamento,
        Map<String, Integer> porLogistica
) {}
