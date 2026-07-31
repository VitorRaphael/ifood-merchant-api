package com.vitorraphael.ifood.merchant.api.model;

import java.math.BigDecimal;

public record RankingProduto(
        String nome,
        Long quantidadeTotal,
        BigDecimal valorTotal
) {}
