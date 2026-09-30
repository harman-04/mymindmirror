package com.mymindmirror.backend.controller;

import com.mymindmirror.backend.annotation.CurrentUser;
import com.mymindmirror.backend.mapper.MilestoneMapper;
import com.mymindmirror.backend.model.Milestone;
import com.mymindmirror.backend.model.Task;
import com.mymindmirror.backend.model.User;
import com.mymindmirror.backend.payload.request.ImportGrowthTipRequest;
import com.mymindmirror.backend.payload.request.MilestoneRequest;
import com.mymindmirror.backend.payload.response.MessageResponse;
import com.mymindmirror.backend.payload.response.MilestoneInsightResponse;
import com.mymindmirror.backend.payload.response.MilestoneResponse;
import com.mymindmirror.backend.service.*;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/milestones")
@RequiredArgsConstructor
@Slf4j
public class MilestoneController {

    private final MilestoneService milestoneService;
    private final MilestoneInsightService milestoneInsightService;
    private final MilestoneMapper milestoneMapper;

    @PostMapping
    public ResponseEntity<MilestoneResponse> createMilestone(
            @CurrentUser User currentUser,
            @Valid @RequestBody MilestoneRequest milestoneRequest) {

        MilestoneResponse response = milestoneService.createMilestoneAsDTO(
                currentUser,
                milestoneRequest.title(),
                milestoneRequest.description(),
                milestoneRequest.dueDate()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<MilestoneResponse>> getAllMilestones(@CurrentUser User currentUser) {
        List<MilestoneResponse> milestones = milestoneService.getAllMilestonesForUserAsDTO(currentUser);
        return ResponseEntity.ok(milestones);
    }


    @GetMapping("/{id}")
    public ResponseEntity<MilestoneResponse> getMilestoneById(
            @CurrentUser User currentUser,
            @PathVariable UUID id) {

        //  No try-catch – use Optional and ResponseStatusException
        MilestoneResponse response = milestoneService.getMilestoneByIdForUser(id, currentUser)
                .map(milestoneMapper::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Milestone not found or not owned."));

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<MilestoneResponse> updateMilestone(
            @CurrentUser User currentUser,
            @PathVariable UUID id,
            @Valid @RequestBody MilestoneRequest milestoneRequest) {

        MilestoneResponse response = milestoneService.updateMilestoneAsDTO(
                id, currentUser,
                milestoneRequest.title(),
                milestoneRequest.description(),
                milestoneRequest.dueDate(),
                milestoneRequest.status()
        );
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<MessageResponse> deleteMilestone(
            @CurrentUser User currentUser,
            @PathVariable UUID id) {

        milestoneService.deleteMilestone(id, currentUser);
        return ResponseEntity.ok(new MessageResponse("Milestone deleted successfully!"));
    }

    @GetMapping("/{id}/insights")
    public ResponseEntity<MilestoneInsightResponse> getMilestoneInsights(
            @CurrentUser User currentUser,
            @PathVariable UUID id) {

        Milestone milestone = milestoneService.getMilestoneByIdForUser(id, currentUser)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Milestone not found or not owned."));

        try {
            MilestoneInsightResponse insights = milestoneInsightService.getMilestoneInsights(milestone);
            return ResponseEntity.ok(insights);
        } catch (Exception e) {
            log.error("Error fetching insights: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to generate milestone insights.");
        }
    }



    @PostMapping("/import-growth-tip")
    public ResponseEntity<MessageResponse> importGrowthTip(
            @CurrentUser User currentUser,
            @Valid @RequestBody ImportGrowthTipRequest request) {

        List<Task> tasks = milestoneService.importGrowthTipAsTask(currentUser, request.tipText());
        return ResponseEntity.ok(new MessageResponse(tasks.size() + " tasks added to Milestones"));
    }
}