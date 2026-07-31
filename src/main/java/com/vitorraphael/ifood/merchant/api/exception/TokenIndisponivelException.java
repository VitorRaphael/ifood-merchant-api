package com.vitorraphael.ifood.merchant.api.exception;

public class TokenIndisponivelException extends RuntimeException {

    public TokenIndisponivelException(String mensagem) {
        super(mensagem);
    }

    public TokenIndisponivelException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
