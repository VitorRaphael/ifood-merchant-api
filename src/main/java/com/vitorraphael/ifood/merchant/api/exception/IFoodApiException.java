package com.vitorraphael.ifood.merchant.api.exception;

public class IFoodApiException extends RuntimeException {

    private final int statusIfood;

    public IFoodApiException(int statusIfood, String mensagem) {
        super(mensagem);
        this.statusIfood = statusIfood;
    }

    public IFoodApiException(int statusIfood, String mensagem, Throwable causa) {
        super(mensagem, causa);
        this.statusIfood = statusIfood;
    }

    public int getStatusIfood() {
        return statusIfood;
    }
}
