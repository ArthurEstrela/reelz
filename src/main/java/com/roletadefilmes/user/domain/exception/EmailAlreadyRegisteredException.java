package com.roletadefilmes.user.domain.exception;

public class EmailAlreadyRegisteredException extends RuntimeException {

    public EmailAlreadyRegisteredException() {
        super("Já existe uma conta cadastrada com este e-mail.");
    }
}
