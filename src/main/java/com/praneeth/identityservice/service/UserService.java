package com.praneeth.identityservice.service;

import com.praneeth.identityservice.dto.UserResponse;
import com.praneeth.identityservice.entity.User;
import com.praneeth.identityservice.mapper.UserMapper;
import com.praneeth.identityservice.repository.UserRepository;
import com.praneeth.identityservice.security.JwtService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public UserService(UserRepository userRepository,
                       UserMapper userMapper) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Authenticated user was not found"
                ));
        return userMapper.toResponse(user);
    }


}
