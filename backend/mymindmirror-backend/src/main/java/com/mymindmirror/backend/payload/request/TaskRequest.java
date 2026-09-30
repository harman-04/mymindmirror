package com.mymindmirror.backend.payload.request;

import com.mymindmirror.backend.enums.Status;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;
import java.util.List;

public record TaskRequest(
        @NotBlank(message = "Task description cannot be empty")
        String description,

        LocalDate dueDate,

        Status status,

        String details,

        List<String> subtasks
) {}