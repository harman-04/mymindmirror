package com.mymindmirror.backend.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mymindmirror.backend.payload.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Handles authentication failures that happen at the filter chain level
 * (before the request reaches the controller). Returns a 401 Unauthorized
 * with the unified ErrorResponse shape.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        log.warn("Authentication failed for {}: {}", request.getRequestURI(), authException.getMessage());

        ErrorResponse body = ErrorResponse.of(
                401,
                "Unauthorized",
                authException.getMessage(),
                request.getRequestURI()
        );

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}