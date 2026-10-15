package com.mymindmirror.backend.payload.request;

import com.mymindmirror.backend.enums.Status;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;
import java.util.List;

public record TaskRequest(
        @NotBlank(message = "Task description cannot be empty")
        String description,

        LocalDate dueDate,

        @Schema(
                description = "Optional new status.",
                allowableValues = {"PENDING", "IN_PROGRESS", "COMPLETED", "OVERDUE", "CANCELLED"},
                nullable = true)
        Status status,

        String details,

        List<String> subtasks
) {}