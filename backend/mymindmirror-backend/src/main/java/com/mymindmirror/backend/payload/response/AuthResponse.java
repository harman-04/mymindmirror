package com.mymindmirror.backend.payload.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record AuthResponse(
        @Schema(
                description = "JWT token. Present on successful login; null after successful registration.",
                nullable = true)
        String token,

        @Schema(description = "Human-readable status message, e.g. \"Login successful.\".")
        String message
) {}