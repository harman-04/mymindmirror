package com.mymindmirror.backend.payload.response;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RoadmapGenerateResponse(
        String title,
        Integer durationWeeks,
        List<Task> tasks,
        List<Resource> resources,
        List<Milestone> milestones,
        Boolean isFallback
) {
    public record Task(Integer day, Integer week, String description, String type,
                       String details, List<String> subtasks) {}
    public record Resource(String name, String url, String type) {}
    public record Milestone(String name, Integer week) {}
}