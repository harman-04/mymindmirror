package com.mymindmirror.backend.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mymindmirror.backend.payload.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Handles authorization failures at the filter chain level. Returns a 403
 * Forbidden with the unified ErrorResponse shape. In development, an optional
 * debug_message is included for faster troubleshooting.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Value("${app.error.expose-debug:false}")
    private boolean exposeDebugMessage;

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {

        log.error("Access Denied for {}: {}", request.getRequestURI(), accessDeniedException.getMessage(), accessDeniedException);

        ErrorResponse body = exposeDebugMessage
                ? ErrorResponse.of(403, "Forbidden",
                "You do not have permission to access this resource.",
                request.getRequestURI(),
                accessDeniedException.getMessage())
                : ErrorResponse.of(403, "Forbidden",
                "You do not have permission to access this resource.",
                request.getRequestURI());

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}