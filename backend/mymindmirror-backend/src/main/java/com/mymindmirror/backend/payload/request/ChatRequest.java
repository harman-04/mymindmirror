package com.mymindmirror.backend.payload.request;

import jakarta.validation.constraints.NotBlank;

public record ChatRequest(
        @NotBlank(message = "Message cannot be empty")
        String query,

        String sessionId,     // optional — service handles null/blank
        boolean rememberChat  // no validation needed
) {}