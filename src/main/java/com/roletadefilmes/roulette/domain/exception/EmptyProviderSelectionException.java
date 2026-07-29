package com.roletadefilmes.roulette.domain.exception;

public class EmptyProviderSelectionException extends RuntimeException {

    public EmptyProviderSelectionException() {
        super("Selecione pelo menos um serviço de streaming.");
    }
}
