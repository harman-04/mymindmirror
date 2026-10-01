package com.mymindmirror.backend.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InsufficientAuthenticationException;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class JwtAuthenticationEntryPointTest {

    private ObjectMapper objectMapper;
    private JwtAuthenticationEntryPoint entryPoint;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        entryPoint = new JwtAuthenticationEntryPoint(objectMapper);
    }

    @Test
    @DisplayName("commence() writes 401 JSON with the unified shape")
    void commence_writesUnifiedShape() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/protected/resource");
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(request, response, new BadCredentialsException("Invalid JWT"));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).isEqualTo("application/json");

        JsonNode json = objectMapper.readTree(response.getContentAsString(StandardCharsets.UTF_8));

        assertThat(json.get("status").asInt()).isEqualTo(401);
        assertThat(json.get("error").asText()).isEqualTo("Unauthorized");
        assertThat(json.get("message").asText()).isEqualTo("Invalid JWT");
        assertThat(json.get("path").asText()).isEqualTo("/api/protected/resource");
        assertThat(json.has("timestamp")).isTrue();
        assertThat(json.has("debug_message")).isFalse();  // entry point never includes debug
    }

    @Test
    @DisplayName("commence() propagates the exception message")
    void commence_propagatesExceptionMessage() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(request, response,
                new InsufficientAuthenticationException("Full authentication required"));

        JsonNode json = objectMapper.readTree(response.getContentAsString(StandardCharsets.UTF_8));
        assertThat(json.get("message").asText()).isEqualTo("Full authentication required");
    }

    @Test
    @DisplayName("commence() with null exception message omits the message field (NON_NULL)")
    void commence_handlesNullMessage() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(request, response, new BadCredentialsException(null));

        JsonNode json = objectMapper.readTree(response.getContentAsString(StandardCharsets.UTF_8));

        // @JsonInclude(NON_NULL) omits the message field entirely when null.
        // Frontend falls back to `data.error` ("Unauthorized"), which is always present.
        assertThat(json.has("message")).isFalse();
        assertThat(json.get("error").asText()).isEqualTo("Unauthorized");
        assertThat(json.get("status").asInt()).isEqualTo(401);
    }
}