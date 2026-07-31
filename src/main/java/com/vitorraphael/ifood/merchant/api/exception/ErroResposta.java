package com.vitorraphael.ifood.merchant.api.exception;

import java.time.Instant;

public record ErroResposta(
        Instant timestamp,
        int status,
        String erro,
        String mensagem,
        String path
) {}
