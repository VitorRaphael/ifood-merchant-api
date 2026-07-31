package com.vitorraphael.ifood.merchant.api.exception;

public class VendaNaoEncontradaException extends RuntimeException {

    public VendaNaoEncontradaException(String mensagem) {
        super(mensagem);
    }
}
