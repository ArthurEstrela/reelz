package com.roletadefilmes.user.domain.exception;

public class InvalidTimezoneException extends RuntimeException {

    public InvalidTimezoneException() {
        super("Informe um fuso horário IANA válido, como America/Sao_Paulo.");
    }
}
