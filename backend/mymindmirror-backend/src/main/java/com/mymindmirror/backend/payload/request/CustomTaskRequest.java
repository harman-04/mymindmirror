package com.mymindmirror.backend.payload.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record CustomTaskRequest(
        @NotBlank(message = "Title cannot be empty")
        String title,

        String description,

        LocalDate dueDate,

        @NotNull(message = "Estimated hours are required")
        @Schema(description = "Estimated time to complete the task, in hours. Minimum 0.1.")
        Double estimatedHours,

        @Schema(
                description = "Task priority. Defaults to MEDIUM when omitted.",
                allowableValues = {"LOW", "MEDIUM", "HIGH"},
                nullable = true)
        String priority,

        @Schema(description = "Completion flag. Defaults to false when omitted.", nullable = true)
        Boolean completed
) {}