package com.mymindmirror.backend.payload.request;

import com.mymindmirror.backend.enums.Status;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public record MilestoneRequest(
        @NotBlank(message = "Milestone title cannot be empty") // ✅ Ensures title isn't null or blank
        String title,

        String description, // Optional

        LocalDate dueDate,  // Optional

        Status status
) {}