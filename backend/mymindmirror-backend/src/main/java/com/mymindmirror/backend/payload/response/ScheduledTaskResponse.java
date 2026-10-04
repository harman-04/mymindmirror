package com.mymindmirror.backend.payload.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record ScheduledTaskResponse(
        UUID id,
        String title,
        LocalDate scheduledDate,
        LocalTime startTime,
        LocalTime endTime,
        boolean completed,
        @Schema(description = "Set when this block is a roadmap task; null otherwise.", nullable = true)
        UUID roadmapTaskId,

        @Schema(description = "Set when this block is a milestone task; null otherwise.", nullable = true)
        UUID milestoneTaskId,

        @Schema(description = "Set when this block is a custom task; null otherwise.", nullable = true)
        UUID customTaskId,

        @Schema(description = "Task priority.",
                allowableValues = {"LOW", "MEDIUM", "HIGH"})
        String priority,

        @Schema(description = "Type of calendar block.",
                allowableValues = {"WORK_TASK", "ROUTINE", "BREAK", "MEAL"})
        String blockType
) {}