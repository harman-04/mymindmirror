package com.mymindmirror.backend.payload.response;
import java.util.List;

// IMPORTANT: This reuses RoadmapGenerateResponse.Task so we don't duplicate definitions
public record RoadmapContinuationResponse(List<RoadmapGenerateResponse.Task> tasks) {}
