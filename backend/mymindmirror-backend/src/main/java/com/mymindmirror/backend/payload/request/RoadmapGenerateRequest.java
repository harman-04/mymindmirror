package com.mymindmirror.backend.payload.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record RoadmapGenerateRequest(
        @NotBlank(message = "Goal is required")
        String goal,

        @Schema(
                description = "Legacy field. If set, it takes precedence over timeframeValue + timeframeUnit.",
                nullable = true)
        Integer timeframeWeeks, // kept for backward compatibility (weeks)

        @Schema(
                description = "Numeric value paired with timeframeUnit. Ignored if timeframeWeeks is set.",
                nullable = true)
        Integer timeframeValue, //  numeric value
        @Schema(
                description = "Unit for timeframeValue. Ignored if timeframeWeeks is set. " +
                        "Defaults to WEEKS if both timeframeWeeks and (timeframeValue, timeframeUnit) are null.",
                allowableValues = {"DAYS", "WEEKS", "MONTHS", "YEARS"},
                nullable = true)
        String timeframeUnit    // "DAYS", "WEEKS", "MONTHS", "YEARS"
) {}