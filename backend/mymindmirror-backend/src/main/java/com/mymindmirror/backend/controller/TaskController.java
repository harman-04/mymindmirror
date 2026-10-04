package com.mymindmirror.backend.controller;

import com.mymindmirror.backend.annotation.CurrentUser;
import com.mymindmirror.backend.api.TaskApi;
import com.mymindmirror.backend.model.User;
import com.mymindmirror.backend.payload.request.TaskRequest;
import com.mymindmirror.backend.payload.response.MessageResponse;
import com.mymindmirror.backend.payload.response.TaskResponse;
import com.mymindmirror.backend.service.TaskService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/milestones/{milestoneId}/tasks")
@RequiredArgsConstructor
public class TaskController implements TaskApi {

    private final TaskService taskService;

    @Override
    @PostMapping
    public ResponseEntity<TaskResponse> createTask(
            @CurrentUser User currentUser,
            @PathVariable UUID milestoneId,
            @Valid @RequestBody TaskRequest taskRequest) {

        TaskResponse newTask = taskService.createTask(milestoneId, currentUser, taskRequest);
        return new ResponseEntity<>(newTask, HttpStatus.CREATED);
    }

    @Override
    @GetMapping
    public ResponseEntity<List<TaskResponse>> getAllTasksForMilestone(
            @CurrentUser User currentUser,
            @PathVariable UUID milestoneId) {

        List<TaskResponse> tasks = taskService.getAllTasksForMilestone(milestoneId, currentUser);
        return ResponseEntity.ok(tasks);
    }

    @Override
    @GetMapping("/{taskId}")
    public ResponseEntity<TaskResponse> getTaskById(
            @CurrentUser User currentUser,
            @PathVariable UUID milestoneId,
            @PathVariable UUID taskId) {

        return taskService.getTaskByIdForMilestoneAndUser(taskId, milestoneId, currentUser)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Task not found or not owned by user/milestone."));
    }

    @Override
    @PutMapping("/{taskId}")
    public ResponseEntity<TaskResponse> updateTask(
            @CurrentUser User currentUser,
            @PathVariable UUID milestoneId,
            @PathVariable UUID taskId,
            @Valid @RequestBody TaskRequest taskRequest) {

        TaskResponse updatedTask = taskService.updateTask(taskId, milestoneId, currentUser, taskRequest);
        return ResponseEntity.ok(updatedTask);
    }

    @Override
    @DeleteMapping("/{taskId}")
    public ResponseEntity<MessageResponse> deleteTask(
            @CurrentUser User currentUser,
            @PathVariable UUID milestoneId,
            @PathVariable UUID taskId) {

        taskService.deleteTask(taskId, milestoneId, currentUser);
        return ResponseEntity.ok(new MessageResponse("Task deleted successfully!"));
    }
}