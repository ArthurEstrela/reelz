package com.roletadefilmes.streaming.domain.exception;

import java.util.Collection;
import java.util.UUID;

public class InvalidStreamingPreferenceException extends RuntimeException {

    public InvalidStreamingPreferenceException(Collection<UUID> providerIds) {
        super("Um ou mais provedores não estão disponíveis para este usuário: " + providerIds);
    }
}
