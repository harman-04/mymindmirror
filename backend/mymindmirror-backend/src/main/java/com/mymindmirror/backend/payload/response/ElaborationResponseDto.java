package com.mymindmirror.backend.payload.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

public record ElaborationResponseDto(
        UUID id,
        @Schema(description = "Detailed elaboration text. Markdown allowed.")
        String details,

        @Schema(description = "Concrete subtasks to complete this task.")
        List<String> subtasks
) {}