package com.roletadefilmes.account.domain.exception;

public class InvalidAccountTokenException extends RuntimeException {

    public InvalidAccountTokenException() {
        super("Este link é inválido, expirou ou já foi utilizado.");
    }
}
