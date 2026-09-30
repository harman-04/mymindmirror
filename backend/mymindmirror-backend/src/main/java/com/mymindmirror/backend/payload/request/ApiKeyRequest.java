package com.mymindmirror.backend.payload.request;

// Empty/blank apiKey is intentional: clears the user's stored key.
// Do NOT add @NotBlank — it would break the clear-key flow.
public record ApiKeyRequest(String apiKey) {}