// src/main/java/com/mymindmirror/backend/payload/response/TaskResponse.java
package com.mymindmirror.backend.payload.response;
import com.mymindmirror.backend.enums.Status;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record TaskResponse(
        UUID id,
        String description,
        LocalDateTime creationTimestamp,
        LocalDate dueDate,

        @Schema(
                description = "Task status.",
                allowableValues = {"PENDING", "IN_PROGRESS", "COMPLETED", "OVERDUE", "CANCELLED"})
        Status status,

        @Schema(description = "Additional task details. Markdown allowed.", nullable = true)
        String details,

        @Schema(description = "Concrete subtasks for this task.", nullable = true)
        List<String> subtasks,

        UUID roadmapTaskId
) {}