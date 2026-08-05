package com.roletadefilmes.social.domain.exception;

public class SocialRoomAccessDeniedException extends RuntimeException {

    public SocialRoomAccessDeniedException(String message) {
        super(message);
    }
}
