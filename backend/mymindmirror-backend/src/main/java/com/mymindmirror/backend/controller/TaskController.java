package com.mymindmirror.backend.controller;

import com.mymindmirror.backend.annotation.CurrentUser;
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

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/milestones/{milestoneId}/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @PostMapping
    public ResponseEntity<TaskResponse> createTask(
            @CurrentUser User currentUser,
            @PathVariable UUID milestoneId,
            @Valid @RequestBody TaskRequest taskRequest) {

        //  Pass the DTO directly – no manual unpacking!
        TaskResponse newTask = taskService.createTask(milestoneId, currentUser, taskRequest);
        return new ResponseEntity<>(newTask, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<TaskResponse>> getAllTasksForMilestone(
            @CurrentUser User currentUser,
            @PathVariable UUID milestoneId) {

        List<TaskResponse> tasks = taskService.getAllTasksForMilestone(milestoneId, currentUser);
        return ResponseEntity.ok(tasks);
    }

    @GetMapping("/{taskId}")
    public ResponseEntity<TaskResponse> getTaskById(
            @CurrentUser User currentUser,
            @PathVariable UUID milestoneId,
            @PathVariable UUID taskId) {

        return taskService.getTaskByIdForMilestoneAndUser(taskId, milestoneId, currentUser)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found or not owned by user/milestone."));
    }

    @PutMapping("/{taskId}")
    public ResponseEntity<TaskResponse> updateTask(
            @CurrentUser User currentUser,
            @PathVariable UUID milestoneId,
            @PathVariable UUID taskId,
            @Valid @RequestBody TaskRequest taskRequest) {

        //  Pass the DTO directly!
        TaskResponse updatedTask = taskService.updateTask(taskId, milestoneId, currentUser, taskRequest);
        return ResponseEntity.ok(updatedTask);
    }

    @DeleteMapping("/{taskId}")
    public ResponseEntity<MessageResponse> deleteTask(
            @CurrentUser User currentUser,
            @PathVariable UUID milestoneId,
            @PathVariable UUID taskId) {

        taskService.deleteTask(taskId, milestoneId, currentUser);
        return ResponseEntity.ok(new MessageResponse("Task deleted successfully!"));
    }
}