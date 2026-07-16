package com.praneeth.identityservice.controller;

import com.praneeth.identityservice.dto.CompleteRegistrationRequest;
import com.praneeth.identityservice.dto.RegistrationEmailRequest;
import com.praneeth.identityservice.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService authService;
    public AuthController(AuthService authService) {
        this.authService = authService;
    }
    @PostMapping("/registration/request")
    public ResponseEntity<String> requestRegistration(
            @Valid @RequestBody RegistrationEmailRequest request
    ) {
        return ResponseEntity.accepted()
                .body(authService.requestRegistration(request));
    }
    @PostMapping("/registration/complete")
    public ResponseEntity<String> completeRegistration(@Valid @RequestBody CompleteRegistrationRequest request){
        return ResponseEntity.ok(authService.completeRegistration(request));
    }
}
