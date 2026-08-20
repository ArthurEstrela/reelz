package com.roletadefilmes.user.service;

import com.roletadefilmes.account.domain.exception.InvalidCurrentPasswordException;
import com.roletadefilmes.account.persistence.repository.AccountActionTokenRepository;
import com.roletadefilmes.billing.domain.BillingSubscriptionStatus;
import com.roletadefilmes.billing.domain.exception.BillingSubscriptionConflictException;
import com.roletadefilmes.billing.persistence.repository.BillingSubscriptionRepository;
import com.roletadefilmes.user.api.dto.DeleteUserRequest;
import com.roletadefilmes.user.api.dto.UpdateUserRequest;
import com.roletadefilmes.user.api.dto.UserResponse;
import com.roletadefilmes.user.domain.exception.InvalidTimezoneException;
import com.roletadefilmes.user.domain.exception.UserNotFoundException;
import com.roletadefilmes.user.persistence.entity.UserAccountEntity;
import com.roletadefilmes.user.persistence.repository.UserAccountRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.UUID;
import java.util.List;

@Service
public class UserAccountService {

    private final UserAccountRepository userRepository;
    private final AccountActionTokenRepository tokenRepository;
    private final BillingSubscriptionRepository billingSubscriptionRepository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public UserAccountService(
            UserAccountRepository userRepository,
            AccountActionTokenRepository tokenRepository,
            BillingSubscriptionRepository billingSubscriptionRepository,
            PasswordEncoder passwordEncoder,
            Clock clock
    ) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.billingSubscriptionRepository = billingSubscriptionRepository;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public UserResponse get(UUID userId) {
        return response(findUser(userId));
    }

    @Transactional
    public UserResponse update(UUID userId, UpdateUserRequest request) {
        validateTimezone(request.timezone());
        var user = findUser(userId);
        user.updateProfile(request.displayName().trim(), request.timezone(), request.countryCode());
        return response(user);
    }

    @Transactional
    public void delete(UUID userId, DeleteUserRequest request) {
        var user = findUser(userId);
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCurrentPasswordException();
        }
        if (billingSubscriptionRepository.existsByUserIdAndStatusIn(
                userId,
                List.of(
                        BillingSubscriptionStatus.CHECKOUT_PENDING,
                        BillingSubscriptionStatus.ACTIVE,
                        BillingSubscriptionStatus.PAST_DUE
                )
        )) {
            throw new BillingSubscriptionConflictException(
                    "Resolva o checkout ou cancele sua assinatura Premium antes de excluir a conta."
            );
        }
        tokenRepository.deleteByUserId(userId);
        user.anonymizeAndDelete(clock.instant());
    }

    private UserAccountEntity findUser(UUID userId) {
        return userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }

    private UserResponse response(UserAccountEntity user) {
        return new UserResponse(
                user.getId(), user.getDisplayName(), user.getEmail(), user.getPlan(), user.getRole(),
                user.getTimezone(), user.getCountryCode(), user.getEmailVerifiedAt() != null,
                user.getOnboardingCompletedAt() != null, user.getCreatedAt()
        );
    }

    private void validateTimezone(String timezone) {
        try {
            ZoneId.of(timezone);
        } catch (DateTimeException exception) {
            throw new InvalidTimezoneException();
        }
    }
}
