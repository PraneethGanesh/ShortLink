package com.praneeth.apigateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class JwtGatewayFilter extends OncePerRequestFilter {
    private static final Set<String> PUBLIC_PATHS = Set.of(
            "/api/v1/auth/registration/request",
            "/api/v1/auth/registration/validate",
            "/api/v1/auth/registration/complete",
            "/api/v1/auth/login",
            "/api/v1/github/webhooks",
            "/actuator/health"
    );

    private final JwtService jwtService;

    public JwtGatewayFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (isPublicRequest(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        try {
            Claims claims = jwtService.validateAndExtract(authHeader.substring(7));
            Map<String, String> identityHeaders = new HashMap<>();
            identityHeaders.put("X-User-Email", claims.getSubject());
            identityHeaders.put("X-User-Id", claims.get("userId", String.class));
            identityHeaders.put("X-User-Role", claims.get("role", String.class));

            filterChain.doFilter(new GatewayIdentityRequest(request, identityHeaders), response);
        } catch (JwtException | IllegalArgumentException exception) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
        }
    }

    private boolean isPublicRequest(HttpServletRequest request) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String path = request.getRequestURI();
        return PUBLIC_PATHS.contains(path)
                || path.startsWith("/oauth2/")
                || path.startsWith("/login/oauth2/")
                || !path.startsWith("/api/v1/");
    }

    private static class GatewayIdentityRequest extends HttpServletRequestWrapper {
        private final Map<String, String> identityHeaders;

        GatewayIdentityRequest(HttpServletRequest request, Map<String, String> identityHeaders) {
            super(request);
            this.identityHeaders = identityHeaders;
        }

        @Override
        public String getHeader(String name) {
            String value = identityHeaders.get(name);
            return value != null ? value : super.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            String value = identityHeaders.get(name);
            if (value != null) {
                return Collections.enumeration(List.of(value));
            }
            return super.getHeaders(name);
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            Set<String> names = new HashSet<>();
            Enumeration<String> existingNames = super.getHeaderNames();
            while (existingNames.hasMoreElements()) {
                names.add(existingNames.nextElement());
            }
            names.addAll(identityHeaders.keySet());
            return Collections.enumeration(names);
        }
    }
}