package com.praneeth.identityservice.controller;

import com.praneeth.identityservice.dto.*;
import com.praneeth.identityservice.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService authService;
    public AuthController(AuthService authService) {
        this.authService = authService;
    }
    @PostMapping("/registration/request")
    public ResponseEntity<MessageResponse> requestRegistration(
            @Valid @RequestBody RegistrationEmailRequest request
    ) {
        String message= authService.requestRegistration(request);
        return ResponseEntity.accepted()
                .body(new  MessageResponse(message));
    }
    @PostMapping("/registration/complete")
    public ResponseEntity<UserResponse> completeRegistration(@Valid @RequestBody CompleteRegistrationRequest request){
        return ResponseEntity.ok(authService.completeRegistration(request));
    }
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request) {

        return ResponseEntity.ok(
                authService.login(request)
        );
    }
    @GetMapping("/registration/validate")
    public ResponseEntity<RegistrationTokenResponse>
    validateRegistrationToken(
            @RequestParam String token
    ) {
        return ResponseEntity.ok(
                authService.validateRegistrationToken(token)
        );
    }
    @PostMapping("/2fa/setup")
    public ResponseEntity<TotpSetupResponse> setupTotp(
            @RequestHeader("X-User-Email") String email
    ) {
        return ResponseEntity.ok(authService.setupTotp(email));
    }

    @PostMapping("/2fa/enable")
    public ResponseEntity<TotpStatusResponse> enableTotp(
            @RequestHeader("X-User-Email") String email,
            @Valid @RequestBody TotpVerificationRequest request
    ) {
        return ResponseEntity.ok(authService.enableTotp(email, request));
    }

    @PostMapping("/2fa/disable")
    public ResponseEntity<TotpStatusResponse> disableTotp(
            @RequestHeader("X-User-Email") String email,
            @Valid @RequestBody TotpVerificationRequest request
    ) {
        return ResponseEntity.ok(authService.disableTotp(email, request));
    }
}