package com.roletadefilmes.user.api;

import com.roletadefilmes.user.api.dto.RegisterUserRequest;
import com.roletadefilmes.user.api.dto.UserResponse;
import com.roletadefilmes.user.service.UserRegistrationService;
import com.roletadefilmes.user.service.UserAccountService;
import com.roletadefilmes.user.api.dto.UpdateUserRequest;
import com.roletadefilmes.user.api.dto.DeleteUserRequest;
import com.roletadefilmes.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserRegistrationService registrationService;
    private final UserAccountService accountService;

    public UserController(UserRegistrationService registrationService, UserAccountService accountService) {
        this.registrationService = registrationService;
        this.accountService = accountService;
    }

    @PostMapping
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(registrationService.register(request));
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(@AuthenticationPrincipal AuthenticatedUser principal) {
        return ResponseEntity.ok(accountService.get(principal.userId()));
    }

    @PatchMapping("/me")
    public ResponseEntity<UserResponse> update(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody UpdateUserRequest request
    ) {
        return ResponseEntity.ok(accountService.update(principal.userId(), request));
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody DeleteUserRequest request
    ) {
        accountService.delete(principal.userId(), request);
        return ResponseEntity.noContent().build();
    }
}
