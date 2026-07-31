package com.vitorraphael.ifood.merchant.api.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ResumoFinanceiro(
        LocalDate periodoInicio,
        LocalDate periodoFim,
        long totalVendas,
        BigDecimal valorBrutoTotal,
        BigDecimal valorLiquidoTotal,
        BigDecimal comissaoTotal
) {}

