package com.roletadefilmes.social.domain.exception;

public class PremiumSocialRoomRequiredException extends RuntimeException {

    public PremiumSocialRoomRequiredException() {
        super("A criação de salas em grupo é exclusiva do Reelz Premium. Seus convidados podem entrar gratuitamente.");
    }
}
