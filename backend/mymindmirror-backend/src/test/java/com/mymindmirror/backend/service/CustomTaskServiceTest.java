package com.mymindmirror.backend.service;

import com.mymindmirror.backend.enums.GamificationAction;
import com.mymindmirror.backend.mapper.CustomTaskMapper;
import com.mymindmirror.backend.model.CustomTask;
import com.mymindmirror.backend.model.ScheduledTask;
import com.mymindmirror.backend.model.User;
import com.mymindmirror.backend.payload.request.CustomTaskRequest;
import com.mymindmirror.backend.payload.response.CustomTaskResponse;
import com.mymindmirror.backend.repository.CustomTaskRepository;
import com.mymindmirror.backend.repository.ScheduledTaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class CustomTaskServiceTest {

    @Mock
    private CustomTaskRepository customTaskRepository;

    @Mock
    private ScheduledTaskRepository scheduledTaskRepository;

    @Mock
    private GamificationService gamificationService;

    @Mock
    private CustomTaskMapper customTaskMapper;

    @InjectMocks
    private CustomTaskService customTaskService;

    private User user;
    private UUID userId;
    private CustomTask customTask;
    private UUID customTaskId;
    private CustomTaskResponse response;
    private CustomTaskRequest request;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = new User();
        user.setId(userId);
        user.setUsername("testuser");

        customTaskId = UUID.randomUUID();
        customTask = new CustomTask();
        customTask.setId(customTaskId);
        customTask.setUser(user);
        customTask.setTitle("Test Task");
        customTask.setDescription("Description");
        customTask.setDueDate(LocalDate.now().plusDays(3));
        customTask.setEstimatedHours(2.0);
        customTask.setPriority("MEDIUM");
        customTask.setCompleted(false);
        customTask.setCreatedAt(LocalDate.now());

        response = new CustomTaskResponse(
                customTaskId, "Test Task", "Description",
                LocalDate.now().plusDays(3), 2.0, "MEDIUM", false, LocalDate.now()
        );

        request = new CustomTaskRequest(
                "New Task", "New Desc", LocalDate.now().plusDays(5),
                3.0, "HIGH", false
        );
    }

    // ========== getUserTasks Tests ==========

    @Test
    void getUserTasks_shouldReturnListOfDTOS() {
        // Given
        List<CustomTask> tasks = List.of(customTask);
        when(customTaskRepository.findByUserOrderByCreatedAtDesc(user)).thenReturn(tasks);
        when(customTaskMapper.toResponseList(tasks)).thenReturn(List.of(response));

        // When
        List<CustomTaskResponse> result = customTaskService.getUserTasks(user);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isSameAs(response);
    }

    // ========== createCustomTask Tests ==========

    @Test
    void createCustomTask_shouldCreateAndAwardGamification() {
        // Given
        when(customTaskMapper.toEntity(request)).thenReturn(customTask);
        when(customTaskRepository.save(any(CustomTask.class))).thenReturn(customTask);
        when(customTaskMapper.toResponse(customTask)).thenReturn(response);

        // When
        CustomTaskResponse result = customTaskService.createCustomTask(user, request);

        // Then
        assertThat(result).isSameAs(response);
        verify(gamificationService).recordActivity(user, GamificationAction.TASK_CREATE);
        verify(customTaskRepository).save(customTask);
    }

    @Test
    void createCustomTask_shouldSetDefaultsWhenNull() {
        // Given – request with null values
        CustomTaskRequest requestWithNulls = new CustomTaskRequest(
                "Task", null, null, null, null, null
        );

        // 🔥 FIX: Create a truly empty task so the service sees the 'null' values
        CustomTask emptyTask = new CustomTask();
        emptyTask.setTitle("Task");

        when(customTaskMapper.toEntity(requestWithNulls)).thenReturn(emptyTask);
        when(customTaskRepository.save(any(CustomTask.class))).thenReturn(emptyTask);
        when(customTaskMapper.toResponse(emptyTask)).thenReturn(response);

        // When
        customTaskService.createCustomTask(user, requestWithNulls);

        // Then – verify defaults are set on the newly mapped task
        assertThat(emptyTask.getEstimatedHours()).isEqualTo(1.0);
        assertThat(emptyTask.getPriority()).isEqualTo("MEDIUM");
        assertThat(emptyTask.getDescription()).isEqualTo("");
    }

    // ========== updateCustomTask Tests ==========

    @Test
    void updateCustomTask_shouldUpdateAndSyncScheduledTasks() {
        // Given
        CustomTaskRequest updateRequest = new CustomTaskRequest(
                "Updated Task", "Updated Desc", LocalDate.now().plusDays(7),
                4.0, "LOW", true // mark completed
        );

        // 🔥 FIX: Teach the mock mapper how to actually mutate the object in memory
        doAnswer(invocation -> {
            CustomTask target = invocation.getArgument(0);
            CustomTaskRequest req = invocation.getArgument(1);
            target.setTitle(req.title());
            target.setPriority(req.priority());
            target.setCompleted(req.completed());
            return null;
        }).when(customTaskMapper).updateEntity(any(CustomTask.class), any(CustomTaskRequest.class));

        when(customTaskRepository.findById(customTaskId)).thenReturn(Optional.of(customTask));
        when(customTaskRepository.save(any(CustomTask.class))).thenReturn(customTask);
        when(customTaskMapper.toResponse(customTask)).thenReturn(response);

        // Mock linked scheduled tasks
        ScheduledTask scheduledTask1 = new ScheduledTask();
        scheduledTask1.setId(UUID.randomUUID());
        scheduledTask1.setTitle("Old Title");
        scheduledTask1.setPriority("MEDIUM");
        scheduledTask1.setCompleted(false);
        ScheduledTask scheduledTask2 = new ScheduledTask();
        scheduledTask2.setId(UUID.randomUUID());
        scheduledTask2.setTitle("Old Title");
        scheduledTask2.setPriority("MEDIUM");
        scheduledTask2.setCompleted(false);

        when(scheduledTaskRepository.findByCustomTaskId(customTaskId))
                .thenReturn(List.of(scheduledTask1, scheduledTask2));

        // When
        CustomTaskResponse result = customTaskService.updateCustomTask(customTaskId, user, updateRequest);

        // Then – verify fields updated
        assertThat(customTask.getTitle()).isEqualTo("Updated Task");
        assertThat(customTask.isCompleted()).isTrue();

        // Verify scheduled tasks synced
        assertThat(scheduledTask1.getTitle()).isEqualTo("Updated Task");
        assertThat(scheduledTask1.getPriority()).isEqualTo("LOW");
        assertThat(scheduledTask1.isCompleted()).isTrue();
        assertThat(scheduledTask2.isCompleted()).isTrue();

        // Verify gamification awarded when completed
        verify(gamificationService).recordActivity(user, GamificationAction.TASK);
        verify(customTaskRepository).save(customTask);
    }
    @Test
    void updateCustomTask_shouldNotAwardGamification_whenAlreadyCompleted() {
        // Given – task already completed
        customTask.setCompleted(true);
        CustomTaskRequest updateRequest = new CustomTaskRequest(
                "Updated", null, null, null, null, true // still completed
        );
        when(customTaskRepository.findById(customTaskId)).thenReturn(Optional.of(customTask));
        when(customTaskRepository.save(any(CustomTask.class))).thenReturn(customTask);
        when(customTaskMapper.toResponse(customTask)).thenReturn(response);

        // When
        customTaskService.updateCustomTask(customTaskId, user, updateRequest);

        // Then – no gamification awarded
        verify(gamificationService, never()).recordActivity(any(), any());
        verify(customTaskRepository).save(customTask);
    }

    @Test
    void updateCustomTask_shouldThrowException_whenTaskNotFound() {
        // Given
        when(customTaskRepository.findById(customTaskId)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> customTaskService.updateCustomTask(customTaskId, user, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Custom task not found");
    }

    @Test
    void updateCustomTask_shouldThrowException_whenNotAuthorized() {
        // Given
        User otherUser = new User();
        otherUser.setId(UUID.randomUUID());
        customTask.setUser(otherUser);
        when(customTaskRepository.findById(customTaskId)).thenReturn(Optional.of(customTask));

        // When/Then
        assertThatThrownBy(() -> customTaskService.updateCustomTask(customTaskId, user, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Custom task not found");
    }

    // ========== deleteCustomTask Tests ==========

    @Test
    void deleteCustomTask_shouldDeleteAndRemoveLinkedScheduledTasks() {
        // Given
        ScheduledTask linkedTask = new ScheduledTask();
        linkedTask.setId(UUID.randomUUID());
        when(customTaskRepository.findById(customTaskId)).thenReturn(Optional.of(customTask));
        when(scheduledTaskRepository.findByCustomTaskId(customTaskId))
                .thenReturn(List.of(linkedTask));

        // When
        customTaskService.deleteCustomTask(customTaskId, user);

        // Then
        verify(scheduledTaskRepository).deleteAll(List.of(linkedTask));
        verify(customTaskRepository).delete(customTask);
    }

    @Test
    void deleteCustomTask_shouldThrowException_whenTaskNotFound() {
        // Given
        when(customTaskRepository.findById(customTaskId)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> customTaskService.deleteCustomTask(customTaskId, user))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Custom task not found");
    }

    @Test
    void deleteCustomTask_shouldThrowException_whenNotAuthorized() {
        // Given
        User otherUser = new User();
        otherUser.setId(UUID.randomUUID());
        customTask.setUser(otherUser);
        when(customTaskRepository.findById(customTaskId)).thenReturn(Optional.of(customTask));

        // When/Then
        assertThatThrownBy(() -> customTaskService.deleteCustomTask(customTaskId, user))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Custom task not found");
    }

    @Test
    void updateCustomTask_shouldNotAwardGamification_whenWasCompletedAlready() {
        customTask.setCompleted(true);  // was already completed
        CustomTaskRequest updateRequest = new CustomTaskRequest(
                "Updated", null, null, null, null, true);  // still completed

        when(customTaskRepository.findById(customTaskId)).thenReturn(Optional.of(customTask));
        when(customTaskRepository.save(any())).thenReturn(customTask);
        when(customTaskMapper.toResponse(customTask)).thenReturn(response);
        when(scheduledTaskRepository.findByCustomTaskId(customTaskId)).thenReturn(List.of());

        customTaskService.updateCustomTask(customTaskId, user, updateRequest);

        // wasCompleted=true → !wasCompleted=false → no gamification
        verify(gamificationService, never()).recordActivity(any(), any());
    }

    @Test
    @DisplayName("updateCustomTask: incomplete task stays incomplete → no gamification")
    void updateCustomTask_whenTaskStaysIncomplete_doesNotAwardGamification() {
        customTask.setCompleted(false);   // was NOT completed

        CustomTaskRequest updateRequest = new CustomTaskRequest(
                "Updated Title", null, null, null, null, false);   // still not completed

        // Mapper actually mutates the entity, matching production behavior
        doAnswer(invocation -> {
            CustomTask target = invocation.getArgument(0);
            CustomTaskRequest req = invocation.getArgument(1);
            target.setTitle(req.title());
            target.setCompleted(req.completed());
            return null;
        }).when(customTaskMapper).updateEntity(any(CustomTask.class), any(CustomTaskRequest.class));

        when(customTaskRepository.findById(customTaskId)).thenReturn(Optional.of(customTask));
        when(customTaskRepository.save(any(CustomTask.class))).thenReturn(customTask);
        when(customTaskMapper.toResponse(customTask)).thenReturn(response);
        when(scheduledTaskRepository.findByCustomTaskId(customTaskId)).thenReturn(List.of());

        customTaskService.updateCustomTask(customTaskId, user, updateRequest);

        // wasCompleted=false → !wasCompleted=true. saved.isCompleted()=false → whole expression false
        verify(gamificationService, never()).recordActivity(any(), any());
    }
}