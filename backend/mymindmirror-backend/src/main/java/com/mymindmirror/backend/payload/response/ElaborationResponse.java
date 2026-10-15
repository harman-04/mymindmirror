package com.mymindmirror.backend.payload.response;
import java.util.List;

public record ElaborationResponse(String details, List<String> subtasks, Double estimatedHours) {}
