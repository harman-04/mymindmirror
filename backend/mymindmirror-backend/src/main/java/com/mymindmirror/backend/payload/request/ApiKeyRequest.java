package com.mymindmirror.backend.payload.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

// Empty/blank apiKey is intentional: clears the user's stored key.
// Do NOT add @NotBlank — it would break the clear-key flow.
public record ApiKeyRequest(
        @Schema(
                description = "User's personal Gemini API key. Send null or a blank string to clear " +
                        "the stored key and fall back to the shared key.",
                nullable = true)
        @Size(max = 250, message = "API key must not exceed 250 characters")
        String apiKey
) {}