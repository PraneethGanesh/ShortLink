package com.praneeth.identityservice.service;

import com.praneeth.identityservice.dto.UserResponse;
import com.praneeth.identityservice.entity.User;
import com.praneeth.identityservice.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }


}
