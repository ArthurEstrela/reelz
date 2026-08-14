package com.roletadefilmes.account.domain.exception;

public class EmailNotVerifiedException extends RuntimeException {

    public EmailNotVerifiedException() {
        super("Confirme seu e-mail antes de entrar.");
    }
}
