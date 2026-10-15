package com.mymindmirror.backend.payload.request;

import com.mymindmirror.backend.enums.Status;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public record MilestoneRequest(
        @NotBlank(message = "Milestone title cannot be empty") //  Ensures title isn't null or blank
        String title,

        String description, // Optional

        LocalDate dueDate,  // Optional

        @Schema(
                description = "Optional new status.",
                allowableValues = {"PENDING", "IN_PROGRESS", "COMPLETED", "OVERDUE", "CANCELLED"},
                nullable = true)
        Status status
) {}