package com.mymindmirror.backend.controller;


import com.mymindmirror.backend.annotation.CurrentUser;
import com.mymindmirror.backend.model.User;
import com.mymindmirror.backend.payload.request.ImportTaskRequest;
import com.mymindmirror.backend.payload.request.RoadmapGenerateRequest;
import com.mymindmirror.backend.payload.response.ElaborationResponseDto;
import com.mymindmirror.backend.payload.response.RoadmapResponse;
import com.mymindmirror.backend.service.RoadmapService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/roadmap")
@RequiredArgsConstructor
@Slf4j
public class RoadmapController {

    private final RoadmapService roadmapService;

    @PostMapping("/generate")
    public ResponseEntity<RoadmapResponse> generateRoadmap(
            @CurrentUser User currentUser,
            @Valid @RequestBody RoadmapGenerateRequest request)  {

        RoadmapResponse response = roadmapService.generateRoadmapAsDTO(
                currentUser,
                request.goal(),
                request.timeframeWeeks(),
                request.timeframeValue(),
                request.timeframeUnit()
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<RoadmapResponse>> getUserRoadmaps(@CurrentUser User currentUser) {
        List<RoadmapResponse> roadmaps = roadmapService.getUserRoadmaps(currentUser);
        return ResponseEntity.ok(roadmaps);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRoadmap(
            @CurrentUser User currentUser,
            @PathVariable UUID id) {
        roadmapService.deleteRoadmap(id, currentUser);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/import-task")
    public ResponseEntity<String> importTaskToMilestone(
            @CurrentUser User currentUser,
            @Valid @RequestBody ImportTaskRequest request) {
        roadmapService.importTaskToMilestone(request.roadmapId(), request.taskId(), currentUser);
        return ResponseEntity.ok("Task imported successfully");
    }

    @PatchMapping("/task/{taskId}/toggle")
    public ResponseEntity<Void> toggleTaskCompletion(
            @CurrentUser User currentUser,
            @PathVariable UUID taskId) {
        roadmapService.toggleTaskCompletion(taskId, currentUser);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/continue")
    public ResponseEntity<RoadmapResponse> continueRoadmap(
            @CurrentUser User currentUser,
            @PathVariable UUID id) {
        RoadmapResponse response = roadmapService.continueRoadmap(id, currentUser);
        return ResponseEntity.ok(response);
    }


    @PostMapping("/task/{taskId}/elaborate")
    public ResponseEntity<ElaborationResponseDto> elaborateTask(
            @CurrentUser User currentUser,
            @PathVariable UUID taskId,
            @RequestParam(defaultValue = "false") boolean enhance) {

        ElaborationResponseDto response = roadmapService.elaborateTask(taskId, currentUser, enhance);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/reschedule")
    public ResponseEntity<RoadmapResponse> rescheduleRoadmap(
            @CurrentUser User currentUser,
            @PathVariable UUID id) {
        RoadmapResponse response = roadmapService.rescheduleRoadmap(id, currentUser);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/continue-batch")
    public ResponseEntity<RoadmapResponse> continueRoadmapBatch(
            @CurrentUser User currentUser,
            @PathVariable UUID id,
            @RequestParam(required = false) Integer weeksToGenerate) {
        RoadmapResponse response = roadmapService.continueRoadmapBatch(id, currentUser, weeksToGenerate);
        return ResponseEntity.ok(response);
    }
}