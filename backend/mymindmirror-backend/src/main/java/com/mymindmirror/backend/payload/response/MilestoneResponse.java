package com.mymindmirror.backend.payload.response;

import com.mymindmirror.backend.enums.Status;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record MilestoneResponse(
        UUID id,
        String title,
        String description,
        LocalDate creationDate,
        LocalDate dueDate,

        @Schema(
                description = "Milestone status.",
                allowableValues = {"PENDING", "IN_PROGRESS", "COMPLETED", "OVERDUE", "CANCELLED"})
        Status status,

        @Schema(description = "Completion percentage. Range: 0.0 to 100.0.")
        double completionPercentage,

        List<TaskResponse> tasks
) {}