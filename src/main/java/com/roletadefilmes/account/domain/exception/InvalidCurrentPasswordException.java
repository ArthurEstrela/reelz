package com.roletadefilmes.account.domain.exception;

public class InvalidCurrentPasswordException extends RuntimeException {

    public InvalidCurrentPasswordException() {
        super("A senha atual está incorreta.");
    }
}
