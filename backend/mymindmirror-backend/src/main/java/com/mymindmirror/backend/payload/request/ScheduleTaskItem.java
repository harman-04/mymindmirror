package com.mymindmirror.backend.payload.request;

/**
 * Lightweight DTO representing one schedulable task passed to the AI.
 * Previously nested inside the unused outer ScheduleTaskRequest record.
 */
public record ScheduleTaskItem(
        String id,
        String title,
        Double estimatedHours,
        String dueDate,
        String priority
) {}