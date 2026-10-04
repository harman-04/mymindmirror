package com.mymindmirror.backend.payload.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record Anomaly(
        @Schema(description = "Date of the anomaly in yyyy-MM-dd format.")
        String date,

        @Schema(description = "Types of anomaly detected on this date.",
                allowableValues = {"mood", "words"})
        List<String> type,

        @Schema(description = "Human-readable description of the anomaly.")
        String message
) {}