package com.praneeth.identityservice.oauth;

import com.praneeth.identityservice.entity.Role;
import com.praneeth.identityservice.entity.User;
import com.praneeth.identityservice.entity.UserStatus;
import com.praneeth.identityservice.repository.UserRepository;
import com.praneeth.identityservice.security.JwtService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Locale;

@Component
public class GithubOAuth2SuccessHandler implements AuthenticationSuccessHandler {
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final String frontendSuccessUrl;

    public GithubOAuth2SuccessHandler(
            UserRepository userRepository,
            JwtService jwtService,
            @Value("${application.frontend.oauth-success-url:http://localhost:5173/login}")
            String frontendSuccessUrl
    ) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.frontendSuccessUrl = frontendSuccessUrl;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {
        OAuth2User principal = (OAuth2User) authentication.getPrincipal();
        User user = findOrCreateUser(principal);
        String accessToken = jwtService.generateToken(user);

        String redirectUrl = UriComponentsBuilder.fromUriString(frontendSuccessUrl)
                .queryParam("token", accessToken)
                .build()
                .toUriString();

        response.sendRedirect(redirectUrl);
    }

    private User findOrCreateUser(OAuth2User principal) {
        String email = stringAttribute(principal, "email");
        String login = stringAttribute(principal, "login");

        if (email == null || email.isBlank()) {
            email = login + "@github.local";
        }

        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        return userRepository.findByEmail(normalizedEmail)
                .orElseGet(() -> createUser(principal, normalizedEmail));
    }

    private User createUser(OAuth2User principal, String email) {
        String name = stringAttribute(principal, "name");
        if (name == null || name.isBlank()) {
            name = stringAttribute(principal, "login");
        }

        User user = new User();
        user.setEmail(email);
        user.setName(name == null || name.isBlank() ? email : name);
        user.setPasswordHash("GITHUB_OAUTH_USER");
        user.setRole(Role.USER);
        user.setStatus(UserStatus.ACTIVE);
        user.setEnabled(true);
        return userRepository.save(user);
    }

    private String stringAttribute(OAuth2User principal, String name) {
        Object value = principal.getAttribute(name);
        return value == null ? null : value.toString();
    }
}