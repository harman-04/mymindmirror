package com.mymindmirror.backend.payload.request;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ImportTaskRequest(
        @NotNull(message = "Roadmap ID is required")
        UUID roadmapId,

        @NotNull(message = "Task ID is required")
        UUID taskId
) {}