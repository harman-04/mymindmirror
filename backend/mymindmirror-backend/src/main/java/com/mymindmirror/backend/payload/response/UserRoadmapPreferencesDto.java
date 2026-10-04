package com.mymindmirror.backend.payload.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record UserRoadmapPreferencesDto(
        @Schema(description = "Roadmap difficulty.",
                allowableValues = {"BEGINNER", "INTERMEDIATE", "ADVANCED"})
        String difficulty,

        @Schema(description = "Language preference code (e.g. 'en', 'hi', 'es').")
        String languagePreference,

        @Schema(description = "Preferred learning style.",
                allowableValues = {"READING", "VISUAL", "HANDS_ON"})
        String learningStyle,

        @Schema(description = "Hours per week available for roadmap tasks.")
        Integer hoursPerWeek,

        @Schema(description = "When true, the scheduler avoids weekends.")
        boolean avoidWeekends
) {}