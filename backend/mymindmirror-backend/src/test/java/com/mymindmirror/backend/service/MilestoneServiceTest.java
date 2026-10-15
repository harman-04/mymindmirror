package com.mymindmirror.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mymindmirror.backend.enums.AITask;
import com.mymindmirror.backend.enums.GamificationAction;
import com.mymindmirror.backend.enums.Status;
import com.mymindmirror.backend.mapper.MilestoneMapper;
import com.mymindmirror.backend.model.Milestone;
import com.mymindmirror.backend.model.Task;
import com.mymindmirror.backend.model.User;
import com.mymindmirror.backend.payload.response.GrowthTipParseResponse;
import com.mymindmirror.backend.payload.response.GrowthTipTask;
import com.mymindmirror.backend.payload.response.MilestoneResponse;
import com.mymindmirror.backend.repository.MilestoneRepository;
import com.mymindmirror.backend.repository.TaskRepository;
import com.mymindmirror.backend.service.ai.DynamicAiClientService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
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
class MilestoneServiceTest {

    @Mock
    private MilestoneRepository milestoneRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UserService userService;

    @Mock
    private MilestoneMapper milestoneMapper;

    @Mock
    private GamificationService gamificationService;

    @Mock
    private TransactionTemplate transactionTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private DynamicAiClientService aiClientService;

    @InjectMocks
    private MilestoneService milestoneService;

    private User user;
    private UUID userId;
    private Milestone milestone;
    private UUID milestoneId;
    private Task task;
    private MilestoneResponse milestoneResponse;

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
        milestone.setDescription("Description");
        milestone.setStatus(Status.PENDING);
        milestone.setCreationDate(LocalDate.now());

        task = new Task();
        task.setId(UUID.randomUUID());
        task.setMilestone(milestone);
        task.setStatus(Status.PENDING);
        task.setCreationTimestamp(LocalDateTime.now());

        milestoneResponse = new MilestoneResponse(
                milestoneId, "Test Milestone", "Description",
                LocalDate.now(), null, Status.PENDING, 0.0, List.of()
        );
    }

    // ========== createMilestone Tests ==========

    @Test
    void createMilestone_shouldCreateAndAwardBadge() {
        // Given
        String title = "New Milestone";
        String description = "Desc";
        LocalDate dueDate = LocalDate.now().plusDays(7);

        when(milestoneRepository.save(any(Milestone.class))).thenReturn(milestone);

        // When
        Milestone result = milestoneService.createMilestone(user, title, description, dueDate);

        // Then
        assertThat(result).isSameAs(milestone);
        assertThat(result.getStatus()).isEqualTo(Status.PENDING);
        verify(gamificationService).recordActivity(user, GamificationAction.MILESTONE_CREATE);
        verify(milestoneRepository).save(any(Milestone.class));
    }

    @Test
    void createMilestoneAsDTO_shouldReturnDTO() {
        // Given
        when(milestoneRepository.save(any(Milestone.class))).thenReturn(milestone);
        when(milestoneMapper.toResponse(milestone)).thenReturn(milestoneResponse);

        // When
        MilestoneResponse result = milestoneService.createMilestoneAsDTO(
                user, "New", "Desc", LocalDate.now().plusDays(7)
        );

        // Then
        assertThat(result).isSameAs(milestoneResponse);
        verify(milestoneMapper).toResponse(milestone);
    }

    // ========== updateMilestone Tests ==========

    @Test
    void updateMilestone_shouldUpdateFields() {
        // Given
        String newTitle = "Updated Title";
        String newDesc = "Updated Desc";
        LocalDate newDueDate = LocalDate.now().plusDays(14);
        Status newStatus = Status.IN_PROGRESS;

        when(milestoneRepository.findByIdAndUser(milestoneId, user)).thenReturn(List.of(milestone));
        when(milestoneRepository.save(any(Milestone.class))).thenReturn(milestone);

        // When
        Milestone result = milestoneService.updateMilestone(
                milestoneId, user, newTitle, newDesc, newDueDate, newStatus
        );

        // Then
        assertThat(result.getTitle()).isEqualTo(newTitle);
        assertThat(result.getDescription()).isEqualTo(newDesc);
        assertThat(result.getDueDate()).isEqualTo(newDueDate);
        assertThat(result.getStatus()).isEqualTo(newStatus);
        verify(milestoneRepository).save(milestone);
    }

    @Test
    void updateMilestone_shouldThrowException_whenNotFound() {
        // Given
        when(milestoneRepository.findByIdAndUser(milestoneId, user)).thenReturn(List.of());

        // When/Then
        assertThatThrownBy(() -> milestoneService.updateMilestone(
                milestoneId, user, "New", "Desc", null, null
        )).isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Milestone not found");
    }

    @Test
    void updateMilestoneAsDTO_shouldReturnDTO() {
        // Given
        when(milestoneRepository.findByIdAndUser(milestoneId, user)).thenReturn(List.of(milestone));
        when(milestoneRepository.save(any(Milestone.class))).thenReturn(milestone);
        when(milestoneMapper.toResponse(milestone)).thenReturn(milestoneResponse);

        // When
        MilestoneResponse result = milestoneService.updateMilestoneAsDTO(
                milestoneId, user, "New", "Desc", null, Status.IN_PROGRESS
        );

        // Then
        assertThat(result).isSameAs(milestoneResponse);
    }

    // ========== deleteMilestone Tests ==========

    @Test
    void deleteMilestone_shouldDelete_whenExists() {
        // Given
        when(milestoneRepository.findByIdAndUser(milestoneId, user)).thenReturn(List.of(milestone));

        // When
        milestoneService.deleteMilestone(milestoneId, user);

        // Then
        verify(milestoneRepository).delete(milestone);
    }

    @Test
    void deleteMilestone_shouldThrowException_whenNotFound() {
        // Given
        when(milestoneRepository.findByIdAndUser(milestoneId, user)).thenReturn(List.of());

        // When/Then
        assertThatThrownBy(() -> milestoneService.deleteMilestone(milestoneId, user))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Milestone not found");
    }

    // ========== getMilestoneById Tests ==========

    @Test
    void getMilestoneByIdForUser_shouldReturnMilestone() {
        // Given
        when(milestoneRepository.findByIdAndUser(milestoneId, user)).thenReturn(List.of(milestone));

        // When
        Optional<Milestone> result = milestoneService.getMilestoneByIdForUser(milestoneId, user);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isSameAs(milestone);
    }

    @Test
    void getMilestoneByIdForUser_shouldReturnEmpty_whenNotFound() {
        // Given
        when(milestoneRepository.findByIdAndUser(milestoneId, user)).thenReturn(List.of());

        // When
        Optional<Milestone> result = milestoneService.getMilestoneByIdForUser(milestoneId, user);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void getMilestoneResponseById_shouldReturnDTO() {
        // Given
        when(milestoneRepository.findByIdAndUser(milestoneId, user)).thenReturn(List.of(milestone));
        when(milestoneMapper.toResponse(milestone)).thenReturn(milestoneResponse);

        // When
        MilestoneResponse result = milestoneService.getMilestoneResponseById(milestoneId, user);

        // Then
        assertThat(result).isSameAs(milestoneResponse);
    }

    // ========== getAllMilestones Tests ==========

    @Test
    void getAllMilestonesForUser_shouldReturnList() {
        // Given
        List<Milestone> milestones = List.of(milestone);
        when(milestoneRepository.findByUserOrderByCreationDateDesc(user)).thenReturn(milestones);
        when(milestoneMapper.toResponseList(milestones)).thenReturn(List.of(milestoneResponse));

        // When
        List<MilestoneResponse> result = milestoneService.getAllMilestonesForUserAsDTO(user);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isSameAs(milestoneResponse);
    }

    // ========== updateMilestoneStatusBasedOnTasks Tests ==========

    @Test
    void updateMilestoneStatusBasedOnTasks_shouldSetCompleted_whenAllTasksCompleted() {
        // Given
        task.setStatus(Status.COMPLETED);
        milestone.setTasks(List.of(task));

        when(milestoneRepository.findById(milestoneId)).thenReturn(Optional.of(milestone));

        // When
        milestoneService.updateMilestoneStatusBasedOnTasks(milestoneId);

        // Then
        assertThat(milestone.getStatus()).isEqualTo(Status.COMPLETED);
        verify(milestoneRepository).save(milestone);
    }

    @Test
    void updateMilestoneStatusBasedOnTasks_shouldSetInProgress_whenSomeTasksCompleted() {
        // Given
        task.setStatus(Status.COMPLETED); // : Actually complete one task
        Task task2 = new Task();
        task2.setStatus(Status.PENDING);
        milestone.setTasks(List.of(task, task2));

        when(milestoneRepository.findById(milestoneId)).thenReturn(Optional.of(milestone));

        // When
        milestoneService.updateMilestoneStatusBasedOnTasks(milestoneId);

        // Then
        assertThat(milestone.getStatus()).isEqualTo(Status.IN_PROGRESS);
        verify(milestoneRepository).save(milestone);
    }

    @Test
    void updateMilestoneStatusBasedOnTasks_shouldSetOverdue_whenDueDatePassed() {
        // Given
        milestone.setDueDate(LocalDate.now().minusDays(1));
        milestone.setTasks(List.of(task));

        when(milestoneRepository.findById(milestoneId)).thenReturn(Optional.of(milestone));

        // When
        milestoneService.updateMilestoneStatusBasedOnTasks(milestoneId);

        // Then
        assertThat(milestone.getStatus()).isEqualTo(Status.OVERDUE);
        verify(milestoneRepository).save(milestone);
    }

    // ========== getOrCreateMilestoneByTitle Tests ==========

    @Test
    void getOrCreateMilestoneByTitle_shouldReturnExisting() {
        // Given
        String title = "Test Milestone"; //  Must match the title set in setUp()
        List<Milestone> existing = List.of(milestone);
        when(milestoneRepository.findByUserOrderByCreationDateDesc(user)).thenReturn(existing);

        // When
        Milestone result = milestoneService.getOrCreateMilestoneByTitle(user, title);

        // Then
        assertThat(result).isSameAs(milestone);
        verify(milestoneRepository, never()).save(any());
    }

    @Test
    void getOrCreateMilestoneByTitle_shouldCreateNew_whenNotFound() {
        // Given
        String title = "New Milestone";
        when(milestoneRepository.findByUserOrderByCreationDateDesc(user)).thenReturn(List.of());
        when(milestoneRepository.save(any(Milestone.class))).thenReturn(milestone);

        // When
        Milestone result = milestoneService.getOrCreateMilestoneByTitle(user, title);

        // Then
        assertThat(result).isSameAs(milestone);
        verify(milestoneRepository).save(any(Milestone.class));
    }

    // ========== importGrowthTipAsTask Tests ==========

    @Test
    void importGrowthTipAsTask_shouldCreateTasksFromAI() {
        // Given
        String tipText = "Growth tip text";
        GrowthTipParseResponse aiResponse = new GrowthTipParseResponse(
                List.of(
                        new GrowthTipTask("Task 1", "Desc 1", List.of("sub1", "sub2")),
                        new GrowthTipTask("Task 2", "Desc 2", List.of("sub3"))
                )
        );

        when(aiClientService.generateStructured(anyString(), eq(GrowthTipParseResponse.class), eq(userId), eq(AITask.PARSE_GROWTH_TIP)))
                .thenReturn(aiResponse);

        when(milestoneRepository.findByUserOrderByCreationDateDesc(user)).thenReturn(List.of());
        when(milestoneRepository.save(any(Milestone.class))).thenReturn(milestone);

        // Stub transactionTemplate to execute callback
        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<List<Task>> callback = invocation.getArgument(0);
            return callback.doInTransaction(null);
        });

        when(taskRepository.saveAll(anyList())).thenReturn(List.of(task, task));

        // When
        List<Task> result = milestoneService.importGrowthTipAsTask(user, tipText);

        // Then
        assertThat(result).hasSize(2);

        //  Verify that the activity WAS recorded because a new milestone was generated
        verify(gamificationService).recordActivity(user, GamificationAction.MILESTONE_CREATE);

        // Verify that milestone was created and tasks saved
        verify(milestoneRepository).save(any(Milestone.class));
        verify(taskRepository).saveAll(anyList());

        // Verify that due dates were set (default +3 days)
        ArgumentCaptor<List<Task>> captor = ArgumentCaptor.forClass(List.class);
        verify(taskRepository).saveAll(captor.capture());
        List<Task> savedTasks = captor.getValue();
        assertThat(savedTasks.get(0).getDueDate()).isEqualTo(LocalDate.now().plusDays(3));
    }

    @Test
    @DisplayName("importGrowthTipAsTask: key-aware 503 for shared key propagates unchanged")
    void importGrowthTipAsTask_shouldPropagateSharedKey503_whenAIUnavailable() {
        String tipText = "Growth tip text";

        when(aiClientService.generateStructured(anyString(), eq(GrowthTipParseResponse.class), eq(userId), eq(AITask.PARSE_GROWTH_TIP)))
                .thenThrow(new ResponseStatusException(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "The shared AI service is currently at capacity. Please try again shortly, or add your own API key in Profile → Gemini API Key for priority access."));

        assertThatThrownBy(() -> milestoneService.importGrowthTipAsTask(user, tipText))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("shared AI service")
                .hasMessageContaining("add your own API key");
    }

    @Test
    @DisplayName("importGrowthTipAsTask: key-aware 503 for user's own key propagates unchanged")
    void importGrowthTipAsTask_shouldPropagateUserKey503_whenAIUnavailable() {
        String tipText = "Growth tip text";

        when(aiClientService.generateStructured(anyString(), eq(GrowthTipParseResponse.class), eq(userId), eq(AITask.PARSE_GROWTH_TIP)))
                .thenThrow(new ResponseStatusException(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "Your Gemini API key has hit its rate limit. Please try again shortly, or check your quota in Google AI Studio."));

        assertThatThrownBy(() -> milestoneService.importGrowthTipAsTask(user, tipText))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Your Gemini API key");
    }

    @Test
    void updateMilestoneStatusBasedOnTasks_shouldSetPending_whenNoTasksCompleted() {
        // Given – tasks exist but none are completed
        Task task1 = new Task();
        task1.setStatus(Status.PENDING);
        Task task2 = new Task();
        task2.setStatus(Status.PENDING);
        milestone.setTasks(List.of(task1, task2));

        when(milestoneRepository.findById(milestoneId)).thenReturn(Optional.of(milestone));

        // When
        milestoneService.updateMilestoneStatusBasedOnTasks(milestoneId);

        // Then
        assertThat(milestone.getStatus()).isEqualTo(Status.PENDING);
        verify(milestoneRepository).save(milestone);
    }

    @Test
    void importGrowthTipAsTask_shouldThrowException_whenAIResponseEmpty() {
        // Given
        String tipText = "Growth tip text";
        // AI returns null
        when(aiClientService.generateStructured(anyString(), eq(GrowthTipParseResponse.class), eq(userId), eq(AITask.PARSE_GROWTH_TIP)))
                .thenReturn(null);

        // When/Then
        assertThatThrownBy(() -> milestoneService.importGrowthTipAsTask(user, tipText))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("No tasks extracted from growth tip");
    }

    @Test
    void importGrowthTipAsTask_shouldHandleJsonProcessingException() throws Exception {
        // Given
        String tipText = "Growth tip text";
        GrowthTipParseResponse aiResponse = new GrowthTipParseResponse(
                List.of(
                        new GrowthTipTask("Task 1", "Desc 1", List.of("sub1", "sub2"))
                )
        );

        when(aiClientService.generateStructured(anyString(), eq(GrowthTipParseResponse.class), eq(userId), eq(AITask.PARSE_GROWTH_TIP)))
                .thenReturn(aiResponse);

        // Mock objectMapper to throw exception when writing subtasks
        when(objectMapper.writeValueAsString(anyList()))
                .thenThrow(new JsonProcessingException("Mock exception") {});

        when(milestoneRepository.findByUserOrderByCreationDateDesc(user)).thenReturn(List.of());
        when(milestoneRepository.save(any(Milestone.class))).thenReturn(milestone);

        // Stub transactionTemplate to execute callback
        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<List<Task>> callback = invocation.getArgument(0);
            return callback.doInTransaction(null);
        });

        when(taskRepository.saveAll(anyList())).thenReturn(List.of(task));

        // When
        List<Task> result = milestoneService.importGrowthTipAsTask(user, tipText);

        // Then – should not throw, and subtasksJson should be "[]"
        assertThat(result).hasSize(1);
        ArgumentCaptor<List<Task>> captor = ArgumentCaptor.forClass(List.class);
        verify(taskRepository).saveAll(captor.capture());
        List<Task> savedTasks = captor.getValue();
        assertThat(savedTasks.get(0).getSubtasksJson()).isEqualTo("[]");
    }

    @Test
    void importGrowthTipAsTask_shouldSetSubtasksJsonToNull_whenSubtasksEmpty() {
        // Given – subtasks is empty list
        String tipText = "Growth tip text";
        GrowthTipParseResponse aiResponse = new GrowthTipParseResponse(
                List.of(
                        new GrowthTipTask("Task 1", "Desc 1", List.of()) // empty subtasks
                )
        );

        when(aiClientService.generateStructured(anyString(), eq(GrowthTipParseResponse.class), eq(userId), eq(AITask.PARSE_GROWTH_TIP)))
                .thenReturn(aiResponse);

        when(milestoneRepository.findByUserOrderByCreationDateDesc(user)).thenReturn(List.of());
        when(milestoneRepository.save(any(Milestone.class))).thenReturn(milestone);

        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<List<Task>> callback = invocation.getArgument(0);
            return callback.doInTransaction(null);
        });

        when(taskRepository.saveAll(anyList())).thenReturn(List.of(task));

        // When
        List<Task> result = milestoneService.importGrowthTipAsTask(user, tipText);

        // Then – subtasksJson should be null
        assertThat(result).hasSize(1);
        ArgumentCaptor<List<Task>> captor = ArgumentCaptor.forClass(List.class);
        verify(taskRepository).saveAll(captor.capture());
        List<Task> savedTasks = captor.getValue();
        assertThat(savedTasks.get(0).getSubtasksJson()).isNull();
    }

    @Test
    void getMilestoneResponseById_shouldThrowException_whenNotFound() {
        // Given
        when(milestoneRepository.findByIdAndUser(milestoneId, user)).thenReturn(List.of());

        // When/Then
        assertThatThrownBy(() -> milestoneService.getMilestoneResponseById(milestoneId, user))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Milestone not found");
    }

    @Test
    void updateMilestoneStatusBasedOnTasks_shouldSetPending_whenNoTasks() {
        // Given – milestone has no tasks
        milestone.setTasks(List.of());

        when(milestoneRepository.findById(milestoneId)).thenReturn(Optional.of(milestone));

        // When
        milestoneService.updateMilestoneStatusBasedOnTasks(milestoneId);

        // Then
        assertThat(milestone.getStatus()).isEqualTo(Status.PENDING);
        verify(milestoneRepository).save(milestone);
    }

    @Test
    @DisplayName("updateMilestone: null fields are skipped (no overwrites)")
    void updateMilestone_whenAllFieldsNull_skipsAllUpdates() {
        // Milestone starts with known values
        milestone.setTitle("Original");
        milestone.setDescription("Original Desc");
        milestone.setStatus(Status.PENDING);

        when(milestoneRepository.findByIdAndUser(milestoneId, user)).thenReturn(List.of(milestone));
        when(milestoneRepository.save(any(Milestone.class))).thenReturn(milestone);

        Milestone result = milestoneService.updateMilestone(
                milestoneId, user, null, null, null, null);

        // All fields remain untouched
        assertThat(result.getTitle()).isEqualTo("Original");
        assertThat(result.getDescription()).isEqualTo("Original Desc");
        assertThat(result.getStatus()).isEqualTo(Status.PENDING);
    }

    @Test
    @DisplayName("updateMilestone: whitespace-only title is ignored")
    void updateMilestone_whenTitleIsWhitespace_doesNotUpdateTitle() {
        milestone.setTitle("Original");

        when(milestoneRepository.findByIdAndUser(milestoneId, user)).thenReturn(List.of(milestone));
        when(milestoneRepository.save(any(Milestone.class))).thenReturn(milestone);

        Milestone result = milestoneService.updateMilestone(
                milestoneId, user, "   ", "New Desc", null, Status.IN_PROGRESS);

        // Whitespace title ignored, others updated
        assertThat(result.getTitle()).isEqualTo("Original");
        assertThat(result.getDescription()).isEqualTo("New Desc");
        assertThat(result.getStatus()).isEqualTo(Status.IN_PROGRESS);
    }

    @Test
    @DisplayName("updateMilestoneStatusBasedOnTasks: future dueDate skips overdue check")
    void updateMilestoneStatusBasedOnTasks_whenDueDateFuture_doesNotSetOverdue() {
        milestone.setDueDate(LocalDate.now().plusDays(7));   // future
        task.setStatus(Status.COMPLETED);
        milestone.setTasks(List.of(task));

        when(milestoneRepository.findById(milestoneId)).thenReturn(Optional.of(milestone));

        milestoneService.updateMilestoneStatusBasedOnTasks(milestoneId);

        // Status computed from tasks (COMPLETED), not OVERDUE
        assertThat(milestone.getStatus()).isEqualTo(Status.COMPLETED);
    }

    @Test
    @DisplayName("updateMilestoneStatusBasedOnTasks: past dueDate but COMPLETED stays completed")
    void updateMilestoneStatusBasedOnTasks_whenPastDueDateButCompleted_doesNotSetOverdue() {
        milestone.setDueDate(LocalDate.now().minusDays(1));   // past
        milestone.setStatus(Status.COMPLETED);                // already completed
        task.setStatus(Status.COMPLETED);
        milestone.setTasks(List.of(task));

        when(milestoneRepository.findById(milestoneId)).thenReturn(Optional.of(milestone));

        milestoneService.updateMilestoneStatusBasedOnTasks(milestoneId);

        // Remains COMPLETED (not OVERDUE) — third condition short-circuits
        assertThat(milestone.getStatus()).isEqualTo(Status.COMPLETED);
    }

    @Test
    @DisplayName("getOrCreateMilestoneByTitle: non-matching milestone in list falls through to creation")
    void getOrCreateMilestoneByTitle_whenListHasNoMatch_createsNew() {
        Milestone existingDifferent = new Milestone();
        existingDifferent.setId(UUID.randomUUID());
        existingDifferent.setTitle("Some Other Milestone");

        when(milestoneRepository.findByUserOrderByCreationDateDesc(user))
                .thenReturn(List.of(existingDifferent));
        when(milestoneRepository.save(any(Milestone.class))).thenReturn(milestone);

        Milestone result = milestoneService.getOrCreateMilestoneByTitle(user, "Test Milestone");

        // Loop completed without match → fell through to createMilestone
        assertThat(result).isSameAs(milestone);
        verify(milestoneRepository).save(any(Milestone.class));
    }

    @Test
    @DisplayName("importGrowthTipAsTask: response with null tasks → 500")
    void importGrowthTipAsTask_whenTasksIsNull_throws() {
        when(aiClientService.generateStructured(anyString(), eq(GrowthTipParseResponse.class), eq(userId), eq(AITask.PARSE_GROWTH_TIP)))
                .thenReturn(new GrowthTipParseResponse(null));   // ← tasks = null

        assertThatThrownBy(() -> milestoneService.importGrowthTipAsTask(user, "tip"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("No tasks extracted from growth tip");
    }

    @Test
    @DisplayName("importGrowthTipAsTask: response with empty tasks → 500")
    void importGrowthTipAsTask_whenTasksIsEmpty_throws() {
        when(aiClientService.generateStructured(anyString(), eq(GrowthTipParseResponse.class), eq(userId), eq(AITask.PARSE_GROWTH_TIP)))
                .thenReturn(new GrowthTipParseResponse(List.of()));   // ← empty list

        assertThatThrownBy(() -> milestoneService.importGrowthTipAsTask(user, "tip"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("No tasks extracted from growth tip");
    }

    @Test
    @DisplayName("importGrowthTipAsTask: task with null description and null subtasks handled correctly")
    void importGrowthTipAsTask_whenDescriptionAndSubtasksNull_usesEmptyDefaults() {
        GrowthTipParseResponse aiResponse = new GrowthTipParseResponse(
                List.of(new GrowthTipTask("Task 1", null, null))   // ← both null
        );

        when(aiClientService.generateStructured(
                anyString(), eq(GrowthTipParseResponse.class), eq(userId), eq(AITask.PARSE_GROWTH_TIP)))
                .thenReturn(aiResponse);
        when(milestoneRepository.findByUserOrderByCreationDateDesc(user)).thenReturn(List.of());
        when(milestoneRepository.save(any(Milestone.class))).thenReturn(milestone);

        when(transactionTemplate.execute(any())).thenAnswer(inv -> {
            TransactionCallback<List<Task>> cb = inv.getArgument(0);
            return cb.doInTransaction(null);
        });
        when(taskRepository.saveAll(anyList())).thenReturn(List.of(task));

        List<Task> result = milestoneService.importGrowthTipAsTask(user, "tip");

        ArgumentCaptor<List<Task>> captor = ArgumentCaptor.forClass(List.class);
        verify(taskRepository).saveAll(captor.capture());
        Task savedTask = captor.getValue().get(0);

        assertThat(savedTask.getDetails()).isEmpty();
        assertThat(savedTask.getSubtasksJson()).isNull();
    }
}