package com.mymindmirror.backend.payload.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateRoadmapPreferencesRequest(
        @Pattern(regexp = "BEGINNER|INTERMEDIATE|ADVANCED", message = "Difficulty must be BEGINNER, INTERMEDIATE, or ADVANCED")
        String difficulty,

        @Size(min = 2, max = 10, message = "Language preference must be between 2 and 10 characters")
        String languagePreference,

        @Pattern(regexp = "READING|VISUAL|HANDS_ON", message = "Learning style must be READING, VISUAL, or HANDS_ON")
        String learningStyle,

        @Min(value = 1, message = "Hours per week must be at least 1")
        Integer hoursPerWeek,

        Boolean avoidWeekends
) {}