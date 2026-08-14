package com.roletadefilmes.account.api;

import com.roletadefilmes.account.api.dto.AccountEmailRequest;
import com.roletadefilmes.account.api.dto.AccountTokenRequest;
import com.roletadefilmes.account.api.dto.PasswordResetConfirmRequest;
import com.roletadefilmes.account.service.AccountSecurityService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AccountSecurityController {

    private final AccountSecurityService service;

    public AccountSecurityController(AccountSecurityService service) {
        this.service = service;
    }

    @PostMapping("/email-verification/request")
    public ResponseEntity<Void> requestVerification(@Valid @RequestBody AccountEmailRequest request) {
        service.requestVerification(request.email());
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/email-verification/confirm")
    public ResponseEntity<Void> confirmVerification(@Valid @RequestBody AccountTokenRequest request) {
        service.confirmVerification(request.token());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/password-reset/request")
    public ResponseEntity<Void> requestPasswordReset(@Valid @RequestBody AccountEmailRequest request) {
        service.requestPasswordReset(request.email());
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/password-reset/confirm")
    public ResponseEntity<Void> confirmPasswordReset(@Valid @RequestBody PasswordResetConfirmRequest request) {
        service.confirmPasswordReset(request.token(), request.newPassword());
        return ResponseEntity.noContent().build();
    }
}
