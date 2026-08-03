package com.roletadefilmes.analytics.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.roletadefilmes.analytics.api.dto.ProductEventRequest;
import com.roletadefilmes.analytics.domain.ProductEventType;
import com.roletadefilmes.analytics.domain.exception.InvalidProductEventException;
import com.roletadefilmes.analytics.persistence.repository.ProductEventRepository;
import com.roletadefilmes.user.domain.exception.UserNotFoundException;
import com.roletadefilmes.user.persistence.repository.UserAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class ProductEventService {

    private final ProductEventRepository eventRepository;
    private final UserAccountRepository userRepository;
    private final Clock clock;
    private final ObjectMapper objectMapper;

    public ProductEventService(
            ProductEventRepository eventRepository,
            UserAccountRepository userRepository,
            Clock clock,
            ObjectMapper objectMapper
    ) {
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.clock = clock;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void record(UUID userId, ProductEventRequest request) {
        userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        var properties = validatedProperties(request);
        eventRepository.insertIfAbsent(
                UUID.randomUUID(),
                request.eventId(),
                userId,
                request.sessionId(),
                request.eventType().name(),
                toJson(properties),
                Instant.now(clock)
        );
    }

    private String toJson(Map<String, Object> properties) {
        try {
            return objectMapper.writeValueAsString(properties);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize product event", exception);
        }
    }

    private Map<String, Object> validatedProperties(ProductEventRequest request) {
        if (request.eventType() == ProductEventType.WATCH_PROVIDER_CLICKED) {
            if (request.movieId() == null || request.providerId() == null) {
                throw new InvalidProductEventException(
                        "movieId e providerId são obrigatórios ao abrir um provedor"
                );
            }
            Map<String, Object> properties = new LinkedHashMap<>();
            properties.put("movieId", request.movieId());
            properties.put("providerId", request.providerId().toString());
            return properties;
        }

        if (request.movieId() != null || request.providerId() != null) {
            throw new InvalidProductEventException(
                    "Este tipo de evento não aceita informações de filme ou provedor"
            );
        }
        return Map.of();
    }
}
