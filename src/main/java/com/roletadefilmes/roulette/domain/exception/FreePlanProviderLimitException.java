package com.roletadefilmes.roulette.domain.exception;

public class FreePlanProviderLimitException extends RuntimeException {

    public FreePlanProviderLimitException() {
        super("O plano gratuito permite selecionar apenas um serviço de streaming por giro.");
    }
}
