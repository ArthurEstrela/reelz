package com.roletadefilmes.social.domain.exception;

public class SocialRoomNotFoundException extends RuntimeException {

    public SocialRoomNotFoundException() {
        super("Sala não encontrada ou convite inválido.");
    }
}
