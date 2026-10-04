package com.mymindmirror.backend.payload.response;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.UUID;

public record CustomTaskResponse(
        UUID id,
        String title,
        String description,
        LocalDate dueDate,
        Double estimatedHours,

        @Schema(description = "Task priority.", allowableValues = {"LOW", "MEDIUM", "HIGH"})
        String priority,

        @Schema(description = "Completion flag.")
        boolean completed,

        @Schema(description = "Task creation date.")
        LocalDate createdAt) {}