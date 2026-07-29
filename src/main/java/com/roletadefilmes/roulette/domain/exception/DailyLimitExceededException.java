package com.roletadefilmes.roulette.domain.exception;

public class DailyLimitExceededException extends RuntimeException {

    public DailyLimitExceededException() {
        super("Limite diário de giros atingido. Assista a um anúncio ou faça upgrade para continuar.");
    }
}
