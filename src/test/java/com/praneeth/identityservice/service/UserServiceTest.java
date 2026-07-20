package com.praneeth.identityservice.service;

import com.praneeth.identityservice.dto.UserResponse;
import com.praneeth.identityservice.entity.User;
import com.praneeth.identityservice.mapper.UserMapper;
import com.praneeth.identityservice.repository.UserRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserService userService;

    @Test
    void currentUserReturnsAuthenticatedUser() {
        String email = "authenticated@example.com";

        User user = new User();
        user.setEmail(email);
        user.setName("Authenticated User");

        UserResponse expectedResponse = new UserResponse();
        expectedResponse.setEmail(email);
        expectedResponse.setName("Authenticated User");

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(userMapper.toResponse(user)).thenReturn(expectedResponse);

        UserResponse result = userService.getCurrentUser(email);

        assertSame(expectedResponse, result);
        assertEquals(email, result.getEmail());
        verify(userRepository).findByEmail(email);
    }
}