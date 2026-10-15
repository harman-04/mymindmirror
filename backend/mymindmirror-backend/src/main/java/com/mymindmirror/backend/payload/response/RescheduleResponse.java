package com.mymindmirror.backend.payload.response;

import java.util.List;

public record RescheduleResponse(
        Integer newDurationWeeks,
        List<TaskUpdate> tasks
) {
    public record TaskUpdate(
            String taskId,   // String to safely handle AI‑returned IDs
            Integer newWeek
    ) {}
}