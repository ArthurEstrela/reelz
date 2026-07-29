package com.roletadefilmes.roulette.domain.exception;

public class DuplicateSpinException extends RuntimeException {

    public DuplicateSpinException() {
        super("Já existe um giro em processamento ou finalizado para esta chave de idempotência.");
    }
}
