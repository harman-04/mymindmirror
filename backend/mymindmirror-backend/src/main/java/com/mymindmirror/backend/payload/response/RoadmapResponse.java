package com.mymindmirror.backend.payload.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record RoadmapResponse(
        UUID id,
        String title,
        String description,
        LocalDate createdAt,

        @Schema(
                description = "Roadmap status.",
                allowableValues = {"PLANNED", "ACTIVE", "COMPLETED"})
        String status,

        @Schema(description = "Total planned duration of the roadmap in weeks.")
        Integer durationWeeks,

        List<TaskDto> tasks,
        List<ResourceDto> resources,
        List<MilestoneDto> milestones,

        @Schema(description = "Number of weeks that have actual tasks generated so far. May be less " +
                "than durationWeeks when generation is staged.")
        Integer generatedWeeks,

        @Schema(description = "Original duration value as supplied by the user at creation.")
        Integer originalDurationValue,

        @Schema(description = "Original duration unit as supplied by the user.",
                allowableValues = {"DAYS", "WEEKS", "MONTHS", "YEARS"})
        String originalDurationUnit
) {
    public record TaskDto(
            UUID id,
            String description,
            Integer dayNumber,
            Integer weekNumber,
            Boolean completed,

            @Schema(
                    description = "Type of roadmap task.",
                    allowableValues = {"daily", "weekly"})
            String taskType,

            @Schema(description = "Long-form instructions. Markdown allowed.")
            String details,

            List<String> subtasks,
            Boolean importedToMilestone) {}

    public record ResourceDto(UUID id, String name, String url, String type) {}

    public record MilestoneDto(UUID id, String name, Integer weekNumber, Boolean achieved) {}
}