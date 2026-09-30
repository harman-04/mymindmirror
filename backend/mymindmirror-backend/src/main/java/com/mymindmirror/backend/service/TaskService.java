package com.mymindmirror.backend.service;

import com.mymindmirror.backend.enums.GamificationAction;
import com.mymindmirror.backend.enums.Status;
import com.mymindmirror.backend.mapper.TaskMapper;
import com.mymindmirror.backend.model.Milestone;
import com.mymindmirror.backend.model.RoadmapTask;
import com.mymindmirror.backend.model.Task;
import com.mymindmirror.backend.model.User;
import com.mymindmirror.backend.payload.request.TaskRequest;
import com.mymindmirror.backend.payload.response.TaskResponse;
import com.mymindmirror.backend.repository.RoadmapTaskRepository;
import com.mymindmirror.backend.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskService {

    private final TaskRepository taskRepository;
    private final MilestoneService milestoneService;
    private final RoadmapTaskRepository roadmapTaskRepository;
    private final GamificationService gamificationService;
    private final TaskMapper taskMapper;

    // ========================================================================
    // NEW PUBLIC METHODS – Accept TaskRequest DTO directly
    // ========================================================================

    @Transactional
    public TaskResponse createTask(UUID milestoneId, User user, TaskRequest request) {
        Milestone milestone = milestoneService.getMilestoneByIdForUser(milestoneId, user)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Milestone not found or not owned by user."));

        // Map DTO to entity (status is already set to PENDING in the DTO from controller? Actually the controller doesn't send status; we need to set it)
        // To be safe, we'll set status PENDING here if the request has null status.
        Task task = taskMapper.toEntity(request);
        if (task.getStatus() == null) {
            task.setStatus(Status.PENDING);
        }
        task.setMilestone(milestone);

        Task saved = taskRepository.save(task);
        milestoneService.updateMilestoneStatusBasedOnTasks(milestone.getId());
        gamificationService.recordActivity(user, GamificationAction.TASK_CREATE);

        return taskMapper.toResponse(saved);
    }

    @Transactional
    public TaskResponse updateTask(UUID taskId, UUID milestoneId, User user, TaskRequest request) {
        Task existingTask = getTaskEntityByIdForMilestoneAndUser(taskId, milestoneId, user)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Task not found or not owned by user/milestone."));

        //  Capture old status BEFORE MapStruct overwrites it
        Status oldStatus = existingTask.getStatus();

        // Map all fields from the request (including status) to the existing entity
        taskMapper.updateEntity(existingTask, request);

        // Handle status change logic using captured oldStatus and request.status()
        if (request.status() != null) {
            Status newStatus = request.status();
            // Ensure the entity has the new status (already set by mapper, but explicit is safer)
            existingTask.setStatus(newStatus);

            if (oldStatus != Status.COMPLETED && newStatus == Status.COMPLETED) {
                gamificationService.recordActivity(user, GamificationAction.TASK);
                if (existingTask.getRoadmapTaskId() != null) {
                    syncRoadmapTaskCompletion(existingTask.getRoadmapTaskId(), true);
                }
            }
            if (oldStatus == Status.COMPLETED && newStatus != Status.COMPLETED) {
                if (existingTask.getRoadmapTaskId() != null) {
                    syncRoadmapTaskCompletion(existingTask.getRoadmapTaskId(), false);
                }
            }
            milestoneService.updateMilestoneStatusBasedOnTasks(milestoneId);
        }

        Task updated = taskRepository.save(existingTask);
        return taskMapper.toResponse(updated);
    }


    @Transactional(readOnly = true)
    public List<TaskResponse> getAllTasksForMilestone(UUID milestoneId, User user) {
        Milestone milestone = milestoneService.getMilestoneByIdForUser(milestoneId, user)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Milestone not found or not owned by user."));

        return taskRepository.findByMilestoneOrderByCreationTimestampAsc(milestone).stream()
                .map(taskMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Optional<TaskResponse> getTaskByIdForMilestoneAndUser(UUID taskId, UUID milestoneId, User user) {
        Milestone milestone = milestoneService.getMilestoneByIdForUser(milestoneId, user)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Milestone not found or not owned by user."));
        List<Task> tasks = taskRepository.findByIdAndMilestone(taskId, milestone);
        if (tasks.isEmpty()) return Optional.empty();
        return Optional.of(taskMapper.toResponse(tasks.get(0)));
    }

    @Transactional
    public void deleteTask(UUID taskId, UUID milestoneId, User user) {
        log.info("Deleting task {} for milestone {} for user {}", taskId, milestoneId, user.getUsername());
        Task existingTask = getTaskEntityByIdForMilestoneAndUser(taskId, milestoneId, user)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Task not found or not owned by user/milestone."));

        Milestone milestone = existingTask.getMilestone();
        milestone.getTasks().remove(existingTask);
        taskRepository.delete(existingTask);
        milestoneService.updateMilestoneStatusBasedOnTasks(milestone.getId());
    }

    // ========================================================================
    // INTERNAL METHODS – For RoadmapService import (unchanged)
    // ========================================================================

    @Transactional
    public Task createTaskWithRoadmapLink(UUID milestoneId, User user, String description,
                                          LocalDate dueDate, UUID roadmapTaskId,
                                          String details, String subtasksJson) {
        Milestone milestone = milestoneService.getMilestoneByIdForUser(milestoneId, user)
                .orElseThrow(() -> new IllegalArgumentException("Milestone not found or not owned by user."));
        Task task = new Task(milestone, description, dueDate);
        task.setStatus(Status.PENDING);
        task.setRoadmapTaskId(roadmapTaskId);
        task.setDetails(details);
        task.setSubtasksJson(subtasksJson);
        Task savedTask = taskRepository.save(task);
        milestoneService.updateMilestoneStatusBasedOnTasks(milestone.getId());
        return savedTask;
    }


    // ========================================================================
    // PRIVATE HELPERS
    // ========================================================================

    private Optional<Task> getTaskEntityByIdForMilestoneAndUser(UUID taskId, UUID milestoneId, User user) {
        Milestone milestone = milestoneService.getMilestoneByIdForUser(milestoneId, user)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Milestone not found or not owned by user."));
        List<Task> tasks = taskRepository.findByIdAndMilestone(taskId, milestone);
        return tasks.isEmpty() ? Optional.empty() : Optional.of(tasks.get(0));
    }

    private void syncRoadmapTaskCompletion(UUID roadmapTaskId, boolean completed) {
        RoadmapTask roadmapTask = roadmapTaskRepository.findById(roadmapTaskId).orElse(null);
        if (roadmapTask != null && roadmapTask.isCompleted() != completed) {
            roadmapTask.setCompleted(completed);
            roadmapTaskRepository.save(roadmapTask);
            log.info("Synced roadmap task {} completion to {}", roadmapTaskId, completed);
        }
    }
}