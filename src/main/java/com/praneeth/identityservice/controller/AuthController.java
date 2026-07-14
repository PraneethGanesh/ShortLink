package com.praneeth.identityservice.controller;

import com.praneeth.identityservice.dto.AuthRequest;
import com.praneeth.identityservice.dto.AuthResponse;
import com.praneeth.identityservice.dto.UserRequest;
import com.praneeth.identityservice.dto.UserResponse;
import com.praneeth.identityservice.entity.User;
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
    @PostMapping("/register")
    public ResponseEntity<UserResponse> registration (@Valid @RequestBody UserRequest userRequest) {
        return ResponseEntity.ok(authService.getRegister(userRequest));
    }
//    @PostMapping("/login")
//    public ResponseEntity<AuthResponse> login (@Valid @RequestBody AuthRequest authRequest) {
//
//    }
}
