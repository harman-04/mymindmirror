package com.mymindmirror.backend.payload.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record ScheduleCustomTaskRequest(
        @NotNull(message = "Custom task ID is required")
        UUID customTaskId,

        @NotBlank(message = "Title is required")
        String title,

        @NotNull(message = "Date is required")
        LocalDate date,

        @NotNull(message = "Start time is required")
        LocalTime startTime,

        @NotNull(message = "End time is required")
        LocalTime endTime,

        @Schema(
                description = "Task priority. Defaults to MEDIUM when omitted.",
                allowableValues = {"LOW", "MEDIUM", "HIGH"},
                nullable = true)
        String priority // optional, will default to MEDIUM
) {}