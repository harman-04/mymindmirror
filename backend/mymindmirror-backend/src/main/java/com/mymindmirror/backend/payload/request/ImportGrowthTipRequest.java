package com.mymindmirror.backend.payload.request;

import jakarta.validation.constraints.NotBlank;

public record ImportGrowthTipRequest(
        @NotBlank(message = "Tip text is required")
        String tipText
) {}