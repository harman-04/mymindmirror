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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private MilestoneService milestoneService;

    @Mock
    private RoadmapTaskRepository roadmapTaskRepository;

    @Mock
    private GamificationService gamificationService;

    @Mock
    private TaskMapper taskMapper;

    @InjectMocks
    private TaskService taskService;

    private User user;
    private UUID userId;
    private Milestone milestone;
    private UUID milestoneId;
    private Task task;
    private UUID taskId;
    private TaskResponse taskResponse;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = new User();
        user.setId(userId);
        user.setUsername("testuser");

        milestoneId = UUID.randomUUID();
        milestone = new Milestone();
        milestone.setId(milestoneId);
        milestone.setUser(user);
        milestone.setTitle("Test Milestone");

        taskId = UUID.randomUUID();
        task = new Task();
        task.setId(taskId);
        task.setMilestone(milestone);
        task.setDescription("Test Task");
        task.setStatus(Status.PENDING);
        task.setCreationTimestamp(LocalDateTime.now());

        taskResponse = new TaskResponse(taskId, "Test Task", LocalDateTime.now(), null, Status.PENDING, null, List.of(), null);
    }

    // ========== createTask Tests ==========

    @Test
    void createTask_shouldCreateAndSaveTask() {
        // Given
        TaskRequest request = new TaskRequest("New Task", LocalDate.now().plusDays(3), Status.PENDING, "Details", List.of("sub1", "sub2"));
        when(milestoneService.getMilestoneByIdForUser(milestoneId, user)).thenReturn(Optional.of(milestone));
        when(taskMapper.toEntity(request)).thenReturn(task);
        when(taskRepository.save(any(Task.class))).thenReturn(task);
        when(taskMapper.toResponse(task)).thenReturn(taskResponse);

        // When
        TaskResponse result = taskService.createTask(milestoneId, user, request);

        // Then
        assertThat(result).isSameAs(taskResponse);
        verify(taskRepository).save(task);
        verify(milestoneService).updateMilestoneStatusBasedOnTasks(milestoneId);
        verify(gamificationService).recordActivity(user, GamificationAction.TASK_CREATE);
    }

    @Test
    void createTask_shouldSetPendingStatusIfNull() {
        // Given
        TaskRequest request = new TaskRequest("New Task", null, null, null, null);
        when(milestoneService.getMilestoneByIdForUser(milestoneId, user)).thenReturn(Optional.of(milestone));
        when(taskMapper.toEntity(request)).thenReturn(task);
        when(taskRepository.save(any(Task.class))).thenReturn(task);
        when(taskMapper.toResponse(task)).thenReturn(taskResponse);

        // When
        taskService.createTask(milestoneId, user, request);

        // Then – status should be set to PENDING in the entity
        assertThat(task.getStatus()).isEqualTo(Status.PENDING);
    }

    @Test
    void createTask_shouldThrowException_whenMilestoneNotFound() {
        // Given
        TaskRequest request = new TaskRequest("New Task", null, null, null, null);
        when(milestoneService.getMilestoneByIdForUser(milestoneId, user)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> taskService.createTask(milestoneId, user, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Milestone not found");
    }

    // ========== updateTask Tests ==========

    @Test
    void updateTask_shouldUpdateAndAwardGamification_whenCompleted() {
        // Given
        task.setStatus(Status.PENDING);
        TaskRequest request = new TaskRequest("Updated Task", LocalDate.now().plusDays(2), Status.COMPLETED, "Updated details", List.of("sub1"));
        when(milestoneService.getMilestoneByIdForUser(milestoneId, user)).thenReturn(Optional.of(milestone));
        // Mock the internal find to return the task
        when(taskRepository.findByIdAndMilestone(taskId, milestone))
                .thenReturn(List.of(task));
        when(taskRepository.save(any(Task.class))).thenReturn(task);
        when(taskMapper.toResponse(task)).thenReturn(taskResponse);

        // When
        TaskResponse result = taskService.updateTask(taskId, milestoneId, user, request);

        // Then
        ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
        verify(taskRepository).save(captor.capture());
        Task saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(Status.COMPLETED);
        verify(gamificationService).recordActivity(user, GamificationAction.TASK);
        verify(milestoneService).updateMilestoneStatusBasedOnTasks(milestoneId);
    }

    @Test
    void updateTask_shouldSyncRoadmapCompletion_whenTaskHasRoadmapLink() {
        // Given
        UUID roadmapTaskId = UUID.randomUUID();
        task.setRoadmapTaskId(roadmapTaskId);
        task.setStatus(Status.PENDING);
        RoadmapTask roadmapTask = new RoadmapTask();
        roadmapTask.setId(roadmapTaskId);
        roadmapTask.setCompleted(false);

        TaskRequest request = new TaskRequest("Updated", null, Status.COMPLETED, null, null);
        when(milestoneService.getMilestoneByIdForUser(milestoneId, user)).thenReturn(Optional.of(milestone));
        when(taskRepository.findByIdAndMilestone(taskId, milestone)).thenReturn(List.of(task));
        when(roadmapTaskRepository.findById(roadmapTaskId)).thenReturn(Optional.of(roadmapTask));
        when(taskRepository.save(any(Task.class))).thenReturn(task);
        when(taskMapper.toResponse(task)).thenReturn(taskResponse);

        // When
        taskService.updateTask(taskId, milestoneId, user, request);

        // Then – roadmap task should be marked completed
        assertThat(roadmapTask.isCompleted()).isTrue();
        verify(roadmapTaskRepository).save(roadmapTask);
    }

    @Test
    void updateTask_shouldThrowException_whenTaskNotFound() {
        // Given
        TaskRequest request = new TaskRequest("Updated", null, null, null, null);
        when(milestoneService.getMilestoneByIdForUser(milestoneId, user)).thenReturn(Optional.of(milestone));
        when(taskRepository.findByIdAndMilestone(taskId, milestone)).thenReturn(List.of());

        // When/Then
        assertThatThrownBy(() -> taskService.updateTask(taskId, milestoneId, user, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Task not found");
    }

    // ========== deleteTask Tests ==========

    @Test
    void deleteTask_shouldDeleteAndUpdateMilestone() {
        // Given
        when(milestoneService.getMilestoneByIdForUser(milestoneId, user)).thenReturn(Optional.of(milestone));
        when(taskRepository.findByIdAndMilestone(taskId, milestone)).thenReturn(List.of(task));
        // When
        taskService.deleteTask(taskId, milestoneId, user);

        // Then
        verify(taskRepository).delete(task);
        verify(milestoneService).updateMilestoneStatusBasedOnTasks(milestoneId);
    }

    @Test
    void deleteTask_shouldThrowException_whenTaskNotFound() {
        // Given
        when(milestoneService.getMilestoneByIdForUser(milestoneId, user)).thenReturn(Optional.of(milestone));
        when(taskRepository.findByIdAndMilestone(taskId, milestone)).thenReturn(List.of());

        // When/Then
        assertThatThrownBy(() -> taskService.deleteTask(taskId, milestoneId, user))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Task not found");
    }

    // ========== getTasks Tests ==========

    @Test
    void getAllTasksForMilestone_shouldReturnTaskDTOs() {
        // Given
        List<Task> tasks = List.of(task);
        when(milestoneService.getMilestoneByIdForUser(milestoneId, user)).thenReturn(Optional.of(milestone));
        when(taskRepository.findByMilestoneOrderByCreationTimestampAsc(milestone)).thenReturn(tasks);
        when(taskMapper.toResponse(task)).thenReturn(taskResponse);

        // When
        List<TaskResponse> result = taskService.getAllTasksForMilestone(milestoneId, user);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isSameAs(taskResponse);
    }

    @Test
    void getTaskByIdForMilestoneAndUser_shouldReturnTask_whenFound() {
        // Given
        when(milestoneService.getMilestoneByIdForUser(milestoneId, user)).thenReturn(Optional.of(milestone));
        when(taskRepository.findByIdAndMilestone(taskId, milestone)).thenReturn(List.of(task));
        when(taskMapper.toResponse(task)).thenReturn(taskResponse);

        // When
        Optional<TaskResponse> result = taskService.getTaskByIdForMilestoneAndUser(taskId, milestoneId, user);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isSameAs(taskResponse);
    }

    @Test
    void getTaskByIdForMilestoneAndUser_shouldReturnEmpty_whenNotFound() {
        // Given
        when(milestoneService.getMilestoneByIdForUser(milestoneId, user)).thenReturn(Optional.of(milestone));
        when(taskRepository.findByIdAndMilestone(taskId, milestone)).thenReturn(List.of());

        // When
        Optional<TaskResponse> result = taskService.getTaskByIdForMilestoneAndUser(taskId, milestoneId, user);

        // Then
        assertThat(result).isEmpty();
    }

    // ========== createTaskWithRoadmapLink Tests ==========

    @Test
    void createTaskWithRoadmapLink_shouldCreateTaskWithLink() {
        // Given
        UUID roadmapTaskId = UUID.randomUUID();
        when(milestoneService.getMilestoneByIdForUser(milestoneId, user)).thenReturn(Optional.of(milestone));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Task result = taskService.createTaskWithRoadmapLink(milestoneId, user, "Description", null, roadmapTaskId, "Details", "[\"sub1\"]");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getRoadmapTaskId()).isEqualTo(roadmapTaskId);
        assertThat(result.getDetails()).isEqualTo("Details");
        assertThat(result.getSubtasksJson()).isEqualTo("[\"sub1\"]");
        assertThat(result.getStatus()).isEqualTo(Status.PENDING);
        verify(milestoneService).updateMilestoneStatusBasedOnTasks(milestoneId);
    }

    @Test
    void createTask_shouldSetPendingStatusWhenRequestStatusNull() {
        // Given
        TaskRequest request = new TaskRequest("New Task", null, null, null, null);
        // Create a task with null status (mapper returns null status)
        Task taskWithNullStatus = new Task();
        taskWithNullStatus.setId(UUID.randomUUID());
        taskWithNullStatus.setMilestone(milestone);
        taskWithNullStatus.setDescription("New Task");
        taskWithNullStatus.setStatus(null); // explicitly null

        when(milestoneService.getMilestoneByIdForUser(milestoneId, user)).thenReturn(Optional.of(milestone));
        when(taskMapper.toEntity(request)).thenReturn(taskWithNullStatus);
        when(taskRepository.save(any(Task.class))).thenReturn(taskWithNullStatus);
        when(taskMapper.toResponse(any(Task.class))).thenReturn(taskResponse);

        // When
        TaskResponse result = taskService.createTask(milestoneId, user, request);

        // Then – status should be set to PENDING
        assertThat(taskWithNullStatus.getStatus()).isEqualTo(Status.PENDING);
        verify(taskRepository).save(taskWithNullStatus);
    }

    @Test
    void updateTask_shouldSyncRoadmapCompletionFalse_whenUncompleting() {
        // Given
        UUID roadmapTaskId = UUID.randomUUID();
        task.setRoadmapTaskId(roadmapTaskId);
        task.setStatus(Status.COMPLETED); // old status is COMPLETED
        RoadmapTask roadmapTask = new RoadmapTask();
        roadmapTask.setId(roadmapTaskId);
        roadmapTask.setCompleted(true);

        TaskRequest request = new TaskRequest("Updated", null, Status.PENDING, null, null);

        when(milestoneService.getMilestoneByIdForUser(milestoneId, user)).thenReturn(Optional.of(milestone));
        when(taskRepository.findByIdAndMilestone(taskId, milestone)).thenReturn(List.of(task));
        when(roadmapTaskRepository.findById(roadmapTaskId)).thenReturn(Optional.of(roadmapTask));
        when(taskRepository.save(any(Task.class))).thenReturn(task);
        when(taskMapper.toResponse(task)).thenReturn(taskResponse);

        // When
        taskService.updateTask(taskId, milestoneId, user, request);

        // Then – roadmap task should be marked not completed
        assertThat(roadmapTask.isCompleted()).isFalse();
        verify(roadmapTaskRepository).save(roadmapTask);
        // Gamification should NOT be awarded (since it's not completing)
        verify(gamificationService, never()).recordActivity(any(), any());
    }

    // ========== Advanced Coverage Tests for Jacoco Gaps ==========

    @Test
    void updateTask_shouldNotChangeStatus_whenRequestStatusIsNull() {
        // Covers Line 72 false branch: if (request.status() != null)
        TaskRequest request = new TaskRequest("Updated Description", null, null, null, null);
        when(milestoneService.getMilestoneByIdForUser(milestoneId, user)).thenReturn(Optional.of(milestone));
        when(taskRepository.findByIdAndMilestone(taskId, milestone)).thenReturn(List.of(task));
        when(taskRepository.save(any(Task.class))).thenReturn(task);
        when(taskMapper.toResponse(task)).thenReturn(taskResponse);

        taskService.updateTask(taskId, milestoneId, user, request);

        // Status logic and milestone update should be entirely skipped
        verify(gamificationService, never()).recordActivity(any(), any());
        verify(milestoneService, never()).updateMilestoneStatusBasedOnTasks(any());
    }

    @Test
    void updateTask_shouldHandleSameStatus_withoutTriggeringSyncs() {
        // Covers Line 77 & 83 false branches: updating a task without actually changing its status
        task.setStatus(Status.PENDING); // Old status is PENDING
        TaskRequest request = new TaskRequest("Updated", null, Status.PENDING, null, null); // New status is PENDING

        when(milestoneService.getMilestoneByIdForUser(milestoneId, user)).thenReturn(Optional.of(milestone));
        when(taskRepository.findByIdAndMilestone(taskId, milestone)).thenReturn(List.of(task));
        when(taskRepository.save(any(Task.class))).thenReturn(task);
        when(taskMapper.toResponse(task)).thenReturn(taskResponse);

        taskService.updateTask(taskId, milestoneId, user, request);

        verify(gamificationService, never()).recordActivity(any(), any());
        verify(roadmapTaskRepository, never()).findById(any());
        verify(milestoneService).updateMilestoneStatusBasedOnTasks(milestoneId);
    }

    @Test
    void updateTask_shouldNotSyncRoadmapCompletion_whenUncompletingTaskWithNoRoadmapLink() {
        // Covers Line 84 false branch: if (existingTask.getRoadmapTaskId() != null) during un-completion
        task.setRoadmapTaskId(null); // Explicitly no roadmap link
        task.setStatus(Status.COMPLETED); // Old status is COMPLETED

        TaskRequest request = new TaskRequest("Updated", null, Status.PENDING, null, null); // Un-completing it

        when(milestoneService.getMilestoneByIdForUser(milestoneId, user)).thenReturn(Optional.of(milestone));
        when(taskRepository.findByIdAndMilestone(taskId, milestone)).thenReturn(List.of(task));
        when(taskRepository.save(any(Task.class))).thenReturn(task);
        when(taskMapper.toResponse(task)).thenReturn(taskResponse);

        taskService.updateTask(taskId, milestoneId, user, request);

        verify(roadmapTaskRepository, never()).findById(any());
    }

    @Test
    void syncRoadmapTaskCompletion_shouldDoNothing_whenRoadmapTaskNotFound() {
        // Covers Line 196 false branch: if (roadmapTask != null ...)
        UUID roadmapTaskId = UUID.randomUUID();
        task.setRoadmapTaskId(roadmapTaskId);
        task.setStatus(Status.PENDING);

        TaskRequest request = new TaskRequest("Updated", null, Status.COMPLETED, null, null);

        when(milestoneService.getMilestoneByIdForUser(milestoneId, user)).thenReturn(Optional.of(milestone));
        when(taskRepository.findByIdAndMilestone(taskId, milestone)).thenReturn(List.of(task));

        // Return empty so roadmapTask evaluates to null in the sync method
        when(roadmapTaskRepository.findById(roadmapTaskId)).thenReturn(Optional.empty());
        when(taskRepository.save(any(Task.class))).thenReturn(task);
        when(taskMapper.toResponse(task)).thenReturn(taskResponse);

        taskService.updateTask(taskId, milestoneId, user, request);

        // Save should never be called on the roadmap task
        verify(roadmapTaskRepository, never()).save(any());
    }

    @Test
    void syncRoadmapTaskCompletion_shouldDoNothing_whenRoadmapTaskAlreadyInCorrectState() {
        // Covers Line 196 false branch: if (... && roadmapTask.isCompleted() != completed)
        UUID roadmapTaskId = UUID.randomUUID();
        task.setRoadmapTaskId(roadmapTaskId);
        task.setStatus(Status.PENDING);

        RoadmapTask roadmapTask = new RoadmapTask();
        roadmapTask.setId(roadmapTaskId);
        // The Roadmap Task is ALREADY completed in the database
        roadmapTask.setCompleted(true);

        TaskRequest request = new TaskRequest("Updated", null, Status.COMPLETED, null, null);

        when(milestoneService.getMilestoneByIdForUser(milestoneId, user)).thenReturn(Optional.of(milestone));
        when(taskRepository.findByIdAndMilestone(taskId, milestone)).thenReturn(List.of(task));
        when(roadmapTaskRepository.findById(roadmapTaskId)).thenReturn(Optional.of(roadmapTask));
        when(taskRepository.save(any(Task.class))).thenReturn(task);
        when(taskMapper.toResponse(task)).thenReturn(taskResponse);

        taskService.updateTask(taskId, milestoneId, user, request);

        // State matches, so save should never be called to prevent redundant DB writes
        verify(roadmapTaskRepository, never()).save(any());
    }

    // ========== Exception Coverage Tests (Milestone Not Found) ==========

    @Test
    void getAllTasksForMilestone_shouldThrowException_whenMilestoneNotFound() {
        when(milestoneService.getMilestoneByIdForUser(milestoneId, user)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.getAllTasksForMilestone(milestoneId, user))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Milestone not found");
    }

    @Test
    void getTaskByIdForMilestoneAndUser_shouldThrowException_whenMilestoneNotFound() {
        when(milestoneService.getMilestoneByIdForUser(milestoneId, user)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.getTaskByIdForMilestoneAndUser(taskId, milestoneId, user))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Milestone not found");
    }

    @Test
    void createTaskWithRoadmapLink_shouldThrowException_whenMilestoneNotFound() {
        UUID roadmapTaskId = UUID.randomUUID();
        when(milestoneService.getMilestoneByIdForUser(milestoneId, user)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.createTaskWithRoadmapLink(
                milestoneId, user, "Description", LocalDate.now(), roadmapTaskId, "Details", "[]"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Milestone not found");
    }

    @Test
    void deleteTask_shouldThrowException_whenMilestoneNotFound() {
        // This covers the private getTaskEntityByIdForMilestoneAndUser() method's exception
        when(milestoneService.getMilestoneByIdForUser(milestoneId, user)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.deleteTask(taskId, milestoneId, user))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Milestone not found");
    }

    @Test
    void updateTask_shouldSyncRoadmapOnUncompletion() {
        UUID roadmapTaskId = UUID.randomUUID();
        task.setRoadmapTaskId(roadmapTaskId);
        task.setStatus(Status.COMPLETED);
        RoadmapTask roadmapTask = new RoadmapTask();
        roadmapTask.setId(roadmapTaskId);
        roadmapTask.setCompleted(true);

        TaskRequest request = new TaskRequest("Updated", null, Status.PENDING, null, null);

        when(milestoneService.getMilestoneByIdForUser(milestoneId, user)).thenReturn(Optional.of(milestone));
        when(taskRepository.findByIdAndMilestone(taskId, milestone)).thenReturn(List.of(task));
        when(roadmapTaskRepository.findById(roadmapTaskId)).thenReturn(Optional.of(roadmapTask));
        when(taskRepository.save(any(Task.class))).thenReturn(task);
        when(taskMapper.toResponse(task)).thenReturn(taskResponse);

        taskService.updateTask(taskId, milestoneId, user, request);

        assertThat(roadmapTask.isCompleted()).isFalse();
    }

    @Test
    @DisplayName("updateTask: old and new status both COMPLETED → no roadmap sync, no gamification")
    void updateTask_whenBothOldAndNewStatusCompleted_doesNotTriggerSync() {
        UUID roadmapTaskId = UUID.randomUUID();
        task.setRoadmapTaskId(roadmapTaskId);
        task.setStatus(Status.COMPLETED);   // old status

        // No-op status update — request keeps it COMPLETED
        TaskRequest request = new TaskRequest("Updated", null, Status.COMPLETED, null, null);

        when(milestoneService.getMilestoneByIdForUser(milestoneId, user)).thenReturn(Optional.of(milestone));
        when(taskRepository.findByIdAndMilestone(taskId, milestone)).thenReturn(List.of(task));
        when(taskRepository.save(any(Task.class))).thenReturn(task);
        when(taskMapper.toResponse(task)).thenReturn(taskResponse);

        taskService.updateTask(taskId, milestoneId, user, request);

        // Second condition: `oldStatus == COMPLETED` = true, `newStatus != COMPLETED` = false → skip
        verify(roadmapTaskRepository, never()).findById(any());
        verify(gamificationService, never()).recordActivity(any(), any());
        verify(milestoneService).updateMilestoneStatusBasedOnTasks(milestoneId);
    }
}