package com.mymindmirror.backend.payload.request;

import jakarta.validation.constraints.NotBlank;

public record JournalEntryRequest(
        @NotBlank(message = "Journal text cannot be empty")
        String rawText) {}