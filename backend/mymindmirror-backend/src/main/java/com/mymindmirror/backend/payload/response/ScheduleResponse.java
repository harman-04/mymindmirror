package com.mymindmirror.backend.payload.response;
import java.util.List;

public record ScheduleResponse(List<ScheduleItem> schedule, List<String> overflow) {}
