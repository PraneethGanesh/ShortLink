package com.praneeth.identityservice.controller;

import com.praneeth.identityservice.dto.UserResponse;
import com.praneeth.identityservice.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(
            @RequestHeader("X-User-Email") String email
    ) {
        return ResponseEntity.ok(userService.getCurrentUser(email));
    }
}