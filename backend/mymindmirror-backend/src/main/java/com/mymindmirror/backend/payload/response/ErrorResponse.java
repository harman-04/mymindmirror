package com.mymindmirror.backend.payload.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

/**
 * Unified error response for the entire application.
 * Used by:
 *   - RestExceptionHandler        (controller-layer errors: 400/401/403/500)
 *   - JwtAuthenticationEntryPoint (filter-layer 401 Unauthorized)
 *   - JwtAccessDeniedHandler      (filter-layer 403 Forbidden)
 *
 * Guarantees a consistent JSON contract across every error path so that
 * clients (React frontend) can rely on a single shape:
 *
 *   { "timestamp", "status", "error", "message", "path", ["debug_message"] }
 *
 * `message` and `error` are ALWAYS present.
 * `debug_message` is only included when {@code app.error.expose-debug=true}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        String path,
        String debug_message
) {
    /**
     * Factory method for production responses (no debug_message).
     */
    public static ErrorResponse of(int status, String error, String message, String path) {
        return new ErrorResponse(LocalDateTime.now(), status, error, message, path, null);
    }

    /**
     * Factory method with debug information. Only call when debug is enabled.
     */
    public static ErrorResponse of(int status, String error, String message, String path, String debugMessage) {
        return new ErrorResponse(LocalDateTime.now(), status, error, message, path, debugMessage);
    }
}