package com.roletadefilmes.shared.api.error;

public record FieldViolation(
        String field,
        String message
) {
}
