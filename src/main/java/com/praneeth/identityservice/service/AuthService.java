package com.praneeth.identityservice.service;

import com.praneeth.identityservice.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final UserRepository userRepository;
    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

//    public UserResponse getRegister(RegisterRequest registerRequest) {
//
//        return userResponse;
//    }
}
