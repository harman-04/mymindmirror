// RoadmapGenerateRequest.java
package com.mymindmirror.backend.payload.request;

import jakarta.validation.constraints.NotBlank;

public record RoadmapGenerateRequest(
        @NotBlank(message = "Goal is required")
        String goal,
        Integer timeframeWeeks, // kept for backward compatibility (weeks)
        Integer timeframeValue, // new: numeric value
        String timeframeUnit    // "DAYS", "WEEKS", "MONTHS", "YEARS"
) {}