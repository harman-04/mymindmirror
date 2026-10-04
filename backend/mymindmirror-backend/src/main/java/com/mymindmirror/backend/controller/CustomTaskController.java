package com.mymindmirror.backend.controller;

import com.mymindmirror.backend.annotation.CurrentUser;
import com.mymindmirror.backend.api.CustomTaskApi;
import com.mymindmirror.backend.model.User;
import com.mymindmirror.backend.payload.request.CustomTaskRequest;
import com.mymindmirror.backend.payload.response.CustomTaskResponse;
import com.mymindmirror.backend.service.CustomTaskService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/custom-tasks")
@RequiredArgsConstructor
public class CustomTaskController implements CustomTaskApi {

    private final CustomTaskService customTaskService;

    @Override
    @GetMapping
    public ResponseEntity<List<CustomTaskResponse>> getUserTasks(@CurrentUser User currentUser) {
        return ResponseEntity.ok(customTaskService.getUserTasks(currentUser));
    }

    @Override
    @PostMapping
    public ResponseEntity<CustomTaskResponse> createCustomTask(
            @CurrentUser User currentUser,
            @Valid @RequestBody CustomTaskRequest request) {
        CustomTaskResponse response = customTaskService.createCustomTask(currentUser, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Override
    @PutMapping("/{id}")
    public ResponseEntity<CustomTaskResponse> updateCustomTask(
            @CurrentUser User currentUser,
            @PathVariable UUID id,
            @Valid @RequestBody CustomTaskRequest request) {
        CustomTaskResponse response = customTaskService.updateCustomTask(id, currentUser, request);
        return ResponseEntity.ok(response);
    }

    @Override
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCustomTask(
            @CurrentUser User currentUser,
            @PathVariable UUID id) {
        customTaskService.deleteCustomTask(id, currentUser);
        return ResponseEntity.noContent().build();
    }
}