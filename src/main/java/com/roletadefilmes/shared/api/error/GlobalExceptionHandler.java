package com.roletadefilmes.shared.api.error;

import com.roletadefilmes.auth.domain.exception.InvalidCredentialsException;
import com.roletadefilmes.roulette.domain.exception.DailyLimitExceededException;
import com.roletadefilmes.roulette.domain.exception.DuplicateSpinException;
import com.roletadefilmes.roulette.domain.exception.EmptyProviderSelectionException;
import com.roletadefilmes.roulette.domain.exception.FreePlanProviderLimitException;
import com.roletadefilmes.roulette.domain.exception.NoMoviesFoundException;
import com.roletadefilmes.user.domain.exception.UserNotFoundException;
import com.roletadefilmes.user.domain.exception.EmailAlreadyRegisteredException;
import com.roletadefilmes.user.domain.exception.InvalidTimezoneException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private final Clock clock;

    public GlobalExceptionHandler(Clock clock) {
        this.clock = clock;
    }

    @ExceptionHandler(NoMoviesFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiErrorResponse handleNoMoviesFound(
            NoMoviesFoundException exception,
            HttpServletRequest request
    ) {
        return error(HttpStatus.NOT_FOUND, "NO_MOVIES_FOUND", exception.getMessage(), request, List.of());
    }

    @ExceptionHandler(DailyLimitExceededException.class)
    @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
    public ApiErrorResponse handleDailyLimitExceeded(
            DailyLimitExceededException exception,
            HttpServletRequest request
    ) {
        return error(
                HttpStatus.TOO_MANY_REQUESTS,
                "DAILY_SPIN_LIMIT_EXCEEDED",
                exception.getMessage(),
                request,
                List.of()
        );
    }

    @ExceptionHandler(UserNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiErrorResponse handleUserNotFound(
            UserNotFoundException exception,
            HttpServletRequest request
    ) {
        return error(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", exception.getMessage(), request, List.of());
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiErrorResponse handleInvalidCredentials(
            InvalidCredentialsException exception,
            HttpServletRequest request
    ) {
        return error(
                HttpStatus.UNAUTHORIZED,
                "INVALID_CREDENTIALS",
                exception.getMessage(),
                request,
                List.of()
        );
    }

    @ExceptionHandler(EmailAlreadyRegisteredException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiErrorResponse handleEmailAlreadyRegistered(
            EmailAlreadyRegisteredException exception,
            HttpServletRequest request
    ) {
        return error(
                HttpStatus.CONFLICT,
                "EMAIL_ALREADY_REGISTERED",
                exception.getMessage(),
                request,
                List.of()
        );
    }

    @ExceptionHandler(InvalidTimezoneException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleInvalidTimezone(
            InvalidTimezoneException exception,
            HttpServletRequest request
    ) {
        return error(
                HttpStatus.BAD_REQUEST,
                "INVALID_TIMEZONE",
                exception.getMessage(),
                request,
                List.of()
        );
    }

    @ExceptionHandler({EmptyProviderSelectionException.class, FreePlanProviderLimitException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleInvalidProviderSelection(
            RuntimeException exception,
            HttpServletRequest request
    ) {
        return error(
                HttpStatus.BAD_REQUEST,
                "INVALID_PROVIDER_SELECTION",
                exception.getMessage(),
                request,
                List.of()
        );
    }

    @ExceptionHandler(DuplicateSpinException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiErrorResponse handleDuplicateSpin(
            DuplicateSpinException exception,
            HttpServletRequest request
    ) {
        return error(HttpStatus.CONFLICT, "DUPLICATE_SPIN", exception.getMessage(), request, List.of());
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiErrorResponse handleOptimisticLockingConflict(
            ObjectOptimisticLockingFailureException exception,
            HttpServletRequest request
    ) {
        return error(
                HttpStatus.CONFLICT,
                "CONCURRENT_SPIN_CONFLICT",
                "Outro giro alterou a franquia simultaneamente. Recarregue o saldo e tente novamente.",
                request,
                List.of()
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        var violations = exception.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> new FieldViolation(fieldError.getField(), fieldError.getDefaultMessage()))
                .toList();
        return error(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_ERROR",
                "A requisição possui campos inválidos.",
                request,
                violations
        );
    }

    private ApiErrorResponse error(
            HttpStatus status,
            String code,
            String message,
            HttpServletRequest request,
            List<FieldViolation> violations
    ) {
        return new ApiErrorResponse(
                Instant.now(clock),
                status.value(),
                status.getReasonPhrase(),
                code,
                message,
                request.getRequestURI(),
                violations
        );
    }
}
