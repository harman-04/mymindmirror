package com.mymindmirror.backend.payload.response;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorResponseTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    @DisplayName("of(...) without debug → debug_message must be null")
    void of_withoutDebug_hasNullDebugMessage() {
        ErrorResponse r = ErrorResponse.of(400, "Bad Request", "Invalid input", "/api/x");

        assertThat(r.status()).isEqualTo(400);
        assertThat(r.error()).isEqualTo("Bad Request");
        assertThat(r.message()).isEqualTo("Invalid input");
        assertThat(r.path()).isEqualTo("/api/x");
        assertThat(r.timestamp()).isBeforeOrEqualTo(LocalDateTime.now());
        assertThat(r.debug_message()).isNull();
    }

    @Test
    @DisplayName("of(...) with debug → debug_message set")
    void of_withDebug_hasDebugMessage() {
        ErrorResponse r = ErrorResponse.of(500, "Internal Server Error", "Oops", "/api/y", "NullPointerException");

        assertThat(r.debug_message()).isEqualTo("NullPointerException");
    }

    @Test
    @DisplayName("@JsonInclude(NON_NULL) omits debug_message when null")
    void json_omitsNullDebugMessage() throws Exception {
        ErrorResponse r = ErrorResponse.of(400, "Bad Request", "Invalid", "/api/x");

        String json = objectMapper.writeValueAsString(r);

        assertThat(json).doesNotContain("debug_message");
        assertThat(json).contains("\"status\":400");
        assertThat(json).contains("\"error\":\"Bad Request\"");
        assertThat(json).contains("\"message\":\"Invalid\"");
        assertThat(json).contains("\"path\":\"/api/x\"");
    }

    @Test
    @DisplayName("@JsonInclude(NON_NULL) includes debug_message when set")
    void json_includesDebugMessageWhenSet() throws Exception {
        ErrorResponse r = ErrorResponse.of(500, "Internal Server Error", "Oops", "/api/y", "NPE");

        String json = objectMapper.writeValueAsString(r);

        assertThat(json).contains("\"debug_message\":\"NPE\"");
    }
}