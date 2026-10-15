package com.mymindmirror.backend.payload.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record ApiKeyStatusResponse(
        @Schema(description = "True when the user has stored their own Gemini API key.")
        boolean usingOwnKey,

        @Schema(
                description = "Masked form of the user's key: last 4 characters visible, prefixed " +
                        "with '••••••••'. Null when no personal key is set.",
                nullable = true)
        String maskedKey,

        @Schema(description = "Human-readable status message for display.")
        String message
) {}