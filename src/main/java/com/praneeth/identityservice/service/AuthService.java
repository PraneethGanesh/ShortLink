package com.praneeth.identityservice.service;

import com.praneeth.identityservice.dto.AuthRequest;
import com.praneeth.identityservice.dto.AuthResponse;
import com.praneeth.identityservice.dto.UserRequest;
import com.praneeth.identityservice.dto.UserResponse;
import com.praneeth.identityservice.entity.User;
import com.praneeth.identityservice.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final UserRepository userRepository;
    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserResponse getRegister(UserRequest userRequest) {
        var user = new User();
        user.setName(userRequest.getName());
        user.setEmail(userRequest.getEmail());
        user.setPasswordHash(userRequest.getPasswordHash());
        user.setConfirmPassword(userRequest.getConfirmPassword());
        var userResponse=userRepository.save(user);
        return userResponse;
    }
}
