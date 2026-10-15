package com.mymindmirror.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mymindmirror.backend.payload.response.ErrorResponse;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.io.IOException;

/**
 * Custom Spring Security filter to intercept requests and validate JWT tokens.
 * It runs once per request.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtRequestFilter extends OncePerRequestFilter {


    private final UserDetailsService userDetailsService; // To load user details by username
    private final JwtUtil jwtUtil; // Our JWT utility class
    private final ObjectMapper objectMapper;


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        final String authorizationHeader = request.getHeader("Authorization");

        String username = null;
        String jwt = null;

        // Check if Authorization header exists and starts with "Bearer "
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            jwt = authorizationHeader.substring(7); // Extract the token string
            try {
                username = jwtUtil.extractUsername(jwt); // Extract username from token
            } catch (Exception e) { // Catch specific JWT exceptions from JwtUtil.validateToken()
                log.warn("JWT token extraction failed or token is invalid: {}", e.getMessage());
            }
        }

        // If username is found and no authentication is currently set in SecurityContext
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = null;
            try {
                userDetails = this.userDetailsService.loadUserByUsername(username);
            } catch (DataAccessResourceFailureException e) {
                // DB is unreachable — treat as a service outage, not an auth failure.
                log.error("Database unavailable during JWT validation for user {}: {}", username, e.getMessage());
                writeServiceUnavailable(response, request);
                return;   // Do NOT continue the chain — we've already written the response.
            } catch (UsernameNotFoundException e) {
                // User genuinely doesn't exist (deleted, wrong username) — proceed unauthenticated.
                log.warn("User not found for username from JWT: {}", username);
            }

            // Validate the token and user details
            if (userDetails != null && jwtUtil.validateToken(jwt, userDetails)) {
                // Create an authentication token
                UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                usernamePasswordAuthenticationToken
                        .setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                // Set the authentication in the SecurityContext
                SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);
                log.info("Authenticated user: {}", username);
            } else {
                log.warn("JWT token validation failed for user: {}", username);
            }
        }

        // Continue the filter chain
        chain.doFilter(request, response);
    }

    private void writeServiceUnavailable(HttpServletResponse response, HttpServletRequest request)
            throws IOException {
        ErrorResponse body = ErrorResponse.of(
                HttpStatus.SERVICE_UNAVAILABLE.value(),
                HttpStatus.SERVICE_UNAVAILABLE.getReasonPhrase(),
                "Authentication service is temporarily unavailable. Please try again in a moment.",
                request.getRequestURI());

        response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
