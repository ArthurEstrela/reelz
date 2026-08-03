package com.roletadefilmes.feedback.service;

import com.roletadefilmes.feedback.api.dto.BetaFeedbackRequest;
import com.roletadefilmes.feedback.persistence.entity.BetaFeedbackEntity;
import com.roletadefilmes.feedback.persistence.repository.BetaFeedbackRepository;
import com.roletadefilmes.user.domain.exception.UserNotFoundException;
import com.roletadefilmes.user.persistence.repository.UserAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
public class BetaFeedbackService {

    private final BetaFeedbackRepository feedbackRepository;
    private final UserAccountRepository userRepository;
    private final Clock clock;

    public BetaFeedbackService(
            BetaFeedbackRepository feedbackRepository,
            UserAccountRepository userRepository,
            Clock clock
    ) {
        this.feedbackRepository = feedbackRepository;
        this.userRepository = userRepository;
        this.clock = clock;
    }

    @Transactional
    public void submit(UUID userId, BetaFeedbackRequest request) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        feedbackRepository.save(new BetaFeedbackEntity(
                user,
                request.score(),
                request.message(),
                Instant.now(clock)
        ));
    }
}
