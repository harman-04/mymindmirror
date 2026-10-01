package com.mymindmirror.backend.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class JwtAccessDeniedHandlerTest {

    private ObjectMapper objectMapper;
    private JwtAccessDeniedHandler handler;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        handler = new JwtAccessDeniedHandler(objectMapper);
    }

    // ------------------------------------------------------------------
    // Production mode (expose-debug = false, the default)
    // ------------------------------------------------------------------

    @Test
    @DisplayName("handle() in prod writes 403 without debug_message")
    void handle_prodMode_omitsDebugMessage() throws Exception {
        ReflectionTestUtils.setField(handler, "exposeDebugMessage", false);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/admin/secret");
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.handle(request, response, new AccessDeniedException("Access is denied"));

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentType()).isEqualTo("application/json");

        JsonNode json = objectMapper.readTree(response.getContentAsString(StandardCharsets.UTF_8));

        assertThat(json.get("status").asInt()).isEqualTo(403);
        assertThat(json.get("error").asText()).isEqualTo("Forbidden");
        assertThat(json.get("message").asText())
                .isEqualTo("You do not have permission to access this resource.");
        assertThat(json.get("path").asText()).isEqualTo("/api/admin/secret");
        assertThat(json.has("timestamp")).isTrue();
        assertThat(json.has("debug_message")).isFalse();
    }

    // ------------------------------------------------------------------
    // Development mode (expose-debug = true)
    // ------------------------------------------------------------------

    @Test
    @DisplayName("handle() in dev includes debug_message")
    void handle_devMode_includesDebugMessage() throws Exception {
        ReflectionTestUtils.setField(handler, "exposeDebugMessage", true);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/admin/secret");
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.handle(request, response, new AccessDeniedException("CSRF token missing"));

        JsonNode json = objectMapper.readTree(response.getContentAsString(StandardCharsets.UTF_8));

        assertThat(json.get("debug_message").asText()).isEqualTo("CSRF token missing");
        assertThat(json.get("status").asInt()).isEqualTo(403);
    }
}