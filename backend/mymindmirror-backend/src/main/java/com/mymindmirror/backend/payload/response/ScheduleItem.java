// src/main/java/com/mymindmirror/backend/payload/response/ScheduleItem.java
package com.mymindmirror.backend.payload.response;

public record ScheduleItem(
        String taskId,
        String title,
        String blockType,
        String date,
        String startTime,
        String endTime
) {}