package com.mymindmirror.backend.payload.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record ChatRequest(
        @NotBlank(message = "Message cannot be empty")
        String query,

        @Schema(
                description = "Session ID for conversation continuity. Optional. When rememberChat is true " +
                        "and this is non-blank, the conversation is stored in Redis for 1 hour.",
                nullable = true)
        String sessionId,     // optional — service handles null/blank

        @Schema(description = "When true, this turn is stored in chat memory. Defaults to false.")
        boolean rememberChat  // no validation needed
) {}