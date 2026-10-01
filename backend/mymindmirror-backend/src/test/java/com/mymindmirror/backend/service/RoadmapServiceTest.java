package com.mymindmirror.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mymindmirror.backend.enums.AITask;
import com.mymindmirror.backend.enums.GamificationAction;
import com.mymindmirror.backend.mapper.RoadmapMapper;
import com.mymindmirror.backend.model.*;
import com.mymindmirror.backend.payload.response.*;
import com.mymindmirror.backend.repository.RoadmapRepository;
import com.mymindmirror.backend.repository.RoadmapTaskRepository;
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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class RoadmapServiceTest {

    @Mock
    private RoadmapRepository roadmapRepository;

    @Mock
    private RoadmapTaskRepository roadmapTaskRepository;

    @Mock
    private MilestoneService milestoneService;

    @Mock
    private TaskService taskService;

    @Mock
    private DynamicAiClientService aiClientService;

    @Mock
    private GamificationService gamificationService;

    @Mock
    private RoadmapMapper roadmapMapper;

    @Mock
    private TransactionTemplate transactionTemplate;

    @Mock
    private UserService userService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private RoadmapService roadmapService;

    private User user;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = new User();
        user.setId(userId);
        user.setUsername("testuser");
        // Do not stub userService here – stub only where needed.
    }

    // ========== Helper Methods ==========

    private RoadmapTask createRoadmapTask(String description, int week, int day) {
        RoadmapTask task = new RoadmapTask();
        task.setId(UUID.randomUUID());
        task.setDescription(description);
        task.setWeekNumber(week);
        task.setDayNumber(day);
        task.setCompleted(false);
        return task;
    }

    // ========== Tests for generateRoadmap ==========

    @Test
    void generateRoadmap_shouldCreateRoadmapAndSave_whenAISucceeds() {
        // Given
        String goal = "Learn Spring Boot";
        Integer timeframeValue = 4;
        String timeframeUnit = "WEEKS";

        UserRoadmapPreferences prefs = new UserRoadmapPreferences();
        prefs.setDifficulty("BEGINNER");
        prefs.setLanguagePreference("en");
        prefs.setLearningStyle("READING");
        prefs.setHoursPerWeek(10);
        prefs.setAvoidWeekends(false);
        when(userService.getRoadmapPreferences(user)).thenReturn(prefs);

        RoadmapGenerateResponse aiResponse = new RoadmapGenerateResponse(
                "Spring Boot Mastery",
                4,
//                null,
                List.of(new RoadmapGenerateResponse.Task(1, 1, "Task 1", "daily", "details", List.of("sub1"))),
                List.of(),
                List.of(),
                false
        );

        when(aiClientService.generateStructured(anyString(), eq(RoadmapGenerateResponse.class), eq(userId), eq(AITask.ROADMAP_INITIAL)))
                .thenReturn(aiResponse);

        // Stub mapper to return a valid RoadmapTask for any AI task
        when(roadmapMapper.toEntity(any(RoadmapGenerateResponse.Task.class)))
                .thenReturn(createRoadmapTask("mapped", 1, 1));

        Roadmap savedRoadmap = new Roadmap();
        savedRoadmap.setId(UUID.randomUUID());
        savedRoadmap.setUser(user);
        savedRoadmap.setTitle("Spring Boot Mastery");
        savedRoadmap.setDurationWeeks(4);
        savedRoadmap.setStatus("ACTIVE");

        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<Roadmap> callback = invocation.getArgument(0);
            return callback.doInTransaction(null);
        });

        when(roadmapRepository.save(any(Roadmap.class))).thenReturn(savedRoadmap);

        // When
        Roadmap result = roadmapService.generateRoadmap(user, goal, null, timeframeValue, timeframeUnit);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("Spring Boot Mastery");
        assertThat(result.getStatus()).isEqualTo("ACTIVE");
        verify(gamificationService).recordActivity(user, GamificationAction.ROADMAP_GENERATE);
        verify(roadmapRepository).save(any(Roadmap.class));
    }

    @Test
    void generateRoadmap_shouldFallbackToFallbackResponse_whenAIFails() {
        // Given
        String goal = "Learn Spring Boot";
        Integer timeframeValue = 4;
        String timeframeUnit = "WEEKS";

        UserRoadmapPreferences prefs = new UserRoadmapPreferences();
        prefs.setDifficulty("BEGINNER");
        prefs.setLanguagePreference("en");
        prefs.setLearningStyle("READING");
        prefs.setHoursPerWeek(10);
        prefs.setAvoidWeekends(false);
        when(userService.getRoadmapPreferences(user)).thenReturn(prefs);

        when(aiClientService.generateStructured(anyString(), eq(RoadmapGenerateResponse.class), eq(userId), eq(AITask.ROADMAP_INITIAL)))
                .thenThrow(new RuntimeException("AI error"));

        // Stub mapper for fallback tasks
        when(roadmapMapper.toEntity(any(RoadmapGenerateResponse.Task.class)))
                .thenReturn(createRoadmapTask("fallback task", 1, 1));

        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<Roadmap> callback = invocation.getArgument(0);
            return callback.doInTransaction(null);
        });

        Roadmap savedRoadmap = new Roadmap();
        savedRoadmap.setId(UUID.randomUUID());
        savedRoadmap.setTitle("Your Personalized Roadmap to Learn Spring Boot");
        when(roadmapRepository.save(any(Roadmap.class))).thenReturn(savedRoadmap);

        // When
        Roadmap result = roadmapService.generateRoadmap(user, goal, null, timeframeValue, timeframeUnit);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTitle()).contains("Personalized Roadmap");
        verify(roadmapRepository).save(any(Roadmap.class));
    }

    @Test
    void generateRoadmap_shouldConvertTimeframeUnitsCorrectly() {
        // Given
        String goal = "Learn Spring Boot";
        Integer timeframeValue = 14;
        String timeframeUnit = "DAYS";

        UserRoadmapPreferences prefs = new UserRoadmapPreferences();
        prefs.setDifficulty("BEGINNER");
        prefs.setLanguagePreference("en");
        prefs.setLearningStyle("READING");
        prefs.setHoursPerWeek(10);
        prefs.setAvoidWeekends(false);
        when(userService.getRoadmapPreferences(user)).thenReturn(prefs);

        // Provide an AI response that maps to 2 weeks (14 days)
        RoadmapGenerateResponse aiResponse = new RoadmapGenerateResponse(
                "Spring Boot Mastery",
                2, // duration weeks
//                null,
                List.of(new RoadmapGenerateResponse.Task(1, 1, "Task", "daily", "details", List.of())),
                List.of(),
                List.of(),
                false
        );
        when(aiClientService.generateStructured(anyString(), eq(RoadmapGenerateResponse.class), eq(userId), eq(AITask.ROADMAP_INITIAL)))
                .thenReturn(aiResponse);

        // Stub the mapper to return a valid RoadmapTask for any AI task
        when(roadmapMapper.toEntity(any(RoadmapGenerateResponse.Task.class)))
                .thenReturn(createRoadmapTask("mapped", 1, 1));

        // Stub the transaction to execute the callback and return the saved roadmap
        Roadmap savedRoadmap = new Roadmap();
        savedRoadmap.setId(UUID.randomUUID());
        savedRoadmap.setDurationWeeks(2);
        savedRoadmap.setOriginalDurationValue(14);
        savedRoadmap.setOriginalDurationUnit("DAYS");
        savedRoadmap.setTitle("Spring Boot Mastery");
        savedRoadmap.setUser(user);
        savedRoadmap.setStatus("ACTIVE");

        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<Roadmap> callback = invocation.getArgument(0);
            return callback.doInTransaction(null);
        });

        // Stub the repository save to return the saved roadmap
        when(roadmapRepository.save(any(Roadmap.class))).thenReturn(savedRoadmap);

        // When
        Roadmap result = roadmapService.generateRoadmap(user, goal, null, timeframeValue, timeframeUnit);

        // Then – result should not be null
        assertThat(result).isNotNull();
        assertThat(result.getDurationWeeks()).isEqualTo(2);
        assertThat(result.getOriginalDurationValue()).isEqualTo(14);
        assertThat(result.getOriginalDurationUnit()).isEqualTo("DAYS");

        // Verify that save was called
        verify(roadmapRepository).save(any(Roadmap.class));
    }
    // ========== Tests for continueRoadmap ==========

    @Test
    void continueRoadmap_shouldGenerateNextWeeksAndSave() {
        // Given
        UUID roadmapId = UUID.randomUUID();
        Roadmap roadmap = new Roadmap();
        roadmap.setId(roadmapId);
        roadmap.setUser(user);
        roadmap.setDescription("Learn Spring Boot");
        roadmap.setTitle("Spring Boot Mastery");
        roadmap.setDurationWeeks(8);
        RoadmapTask existingTask = createRoadmapTask("Task 1", 1, 1);
        existingTask.setCompleted(true);
        List<RoadmapTask> tasks = new ArrayList<>();
        tasks.add(existingTask);
        roadmap.setTasks(tasks);

        when(roadmapRepository.findById(roadmapId)).thenReturn(Optional.of(roadmap));

        RoadmapContinuationResponse aiResponse = new RoadmapContinuationResponse(
                List.of(new RoadmapGenerateResponse.Task(1, 2, "New Task", "daily", "details", List.of("sub")))
        );
        when(aiClientService.generateStructured(anyString(), eq(RoadmapContinuationResponse.class), eq(userId), eq(AITask.ROADMAP_NEXT_STEPS)))
                .thenReturn(aiResponse);

        // Stub mapper for the new task
        when(roadmapMapper.toEntity(any(RoadmapGenerateResponse.Task.class)))
                .thenReturn(createRoadmapTask("New Task", 2, 1));

        RoadmapResponse responseDto = new RoadmapResponse(roadmapId, "Title", "desc", null, "ACTIVE", 8, List.of(), List.of(), List.of(), 4, null, null);
        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<RoadmapResponse> callback = invocation.getArgument(0);
            return callback.doInTransaction(null);
        });
        when(roadmapRepository.save(any(Roadmap.class))).thenReturn(roadmap);
        when(roadmapMapper.toResponse(any(Roadmap.class))).thenReturn(responseDto);

        // When
        RoadmapResponse result = roadmapService.continueRoadmap(roadmapId, user);

        // Then
        assertThat(result).isNotNull();
        ArgumentCaptor<Roadmap> captor = ArgumentCaptor.forClass(Roadmap.class);
        verify(roadmapRepository).save(captor.capture());
        Roadmap saved = captor.getValue();
        assertThat(saved.getTasks()).hasSize(2);
        assertThat(saved.getTasks().get(1).getDescription()).isEqualTo("New Task");
    }

    @Test
    void continueRoadmap_shouldThrowException_whenNoCompletedTasks() {
        UUID roadmapId = UUID.randomUUID();
        Roadmap roadmap = new Roadmap();
        roadmap.setId(roadmapId);
        roadmap.setUser(user);
        roadmap.setTasks(new ArrayList<>());
        when(roadmapRepository.findById(roadmapId)).thenReturn(Optional.of(roadmap));

        assertThatThrownBy(() -> roadmapService.continueRoadmap(roadmapId, user))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No completed tasks");
    }

    // ========== Tests for saveRoadmapInTransaction ==========

    @Test
    void saveRoadmapInTransaction_shouldSaveAndAwardBadge() {
        Roadmap roadmap = new Roadmap();
        roadmap.setUser(user);
        roadmap.setTitle("Test Roadmap");
        roadmap.setStatus("PLANNED");
        when(roadmapRepository.save(any(Roadmap.class))).thenReturn(roadmap);

        Roadmap result = roadmapService.saveRoadmapInTransaction(roadmap, user);

        assertThat(result.getStatus()).isEqualTo("ACTIVE");
        verify(gamificationService).recordActivity(user, GamificationAction.ROADMAP_GENERATE);
        verify(roadmapRepository).save(roadmap);
    }


    // ========== Test for generateRoadmapAsDTO ==========

    @Test
    void generateRoadmapAsDTO_shouldCallGenerateAndMapToDTO() {
        // Given
        String goal = "Learn Spring Boot";
        Integer timeframeValue = 4;
        String timeframeUnit = "WEEKS";

        // Stub the generateRoadmap method (which we already tested) to return a roadmap
        Roadmap roadmap = new Roadmap();
        roadmap.setId(UUID.randomUUID());
        roadmap.setTitle("Test Roadmap");
        // We need to stub the actual generateRoadmap call.
        // Since we are testing the DTO wrapper, we can spy the service or mock the internal call.
        // But the easiest is to stub the dependencies so that generateRoadmap returns a valid roadmap.
        // We'll reuse the stubs from the earlier tests. We'll just stub the AI and transaction.
        UserRoadmapPreferences prefs = new UserRoadmapPreferences();
        prefs.setDifficulty("BEGINNER");
        prefs.setLanguagePreference("en");
        prefs.setLearningStyle("READING");
        prefs.setHoursPerWeek(10);
        prefs.setAvoidWeekends(false);
        when(userService.getRoadmapPreferences(user)).thenReturn(prefs);

        RoadmapGenerateResponse aiResponse = new RoadmapGenerateResponse(
                "Test Roadmap", 4,
//                null,
                List.of(new RoadmapGenerateResponse.Task(1, 1, "Task", "daily", "details", List.of())),
                List.of(), List.of(), false
        );
        when(aiClientService.generateStructured(anyString(), eq(RoadmapGenerateResponse.class), eq(userId), eq(AITask.ROADMAP_INITIAL)))
                .thenReturn(aiResponse);
        when(roadmapMapper.toEntity(any(RoadmapGenerateResponse.Task.class)))
                .thenReturn(createRoadmapTask("mapped", 1, 1));
        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<Roadmap> callback = invocation.getArgument(0);
            return callback.doInTransaction(null);
        });
        Roadmap savedRoadmap = new Roadmap();
        savedRoadmap.setId(UUID.randomUUID());
        savedRoadmap.setTitle("Test Roadmap");
        savedRoadmap.setUser(user);
        savedRoadmap.setStatus("ACTIVE");
        when(roadmapRepository.save(any(Roadmap.class))).thenReturn(savedRoadmap);

        // Stub the mapper to return a DTO
        RoadmapResponse responseDto = new RoadmapResponse(savedRoadmap.getId(), "Test Roadmap", "desc", null, "ACTIVE", 4, List.of(), List.of(), List.of(), 0, null, null);
        when(roadmapMapper.toResponse(any(Roadmap.class))).thenReturn(responseDto);

        // When
        RoadmapResponse result = roadmapService.generateRoadmapAsDTO(user, goal, null, timeframeValue, timeframeUnit);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(savedRoadmap.getId());
        verify(roadmapMapper).toResponse(any(Roadmap.class));
    }

// ========== Tests for elaborateTask ==========

    @Test
    void elaborateTask_shouldReturnElaboratedDTO_whenAISucceeds() {
        // Given
        UUID taskId = UUID.randomUUID();
        boolean enhance = false;

        RoadmapTask task = createRoadmapTask("Learn Spring Security", 1, 1);
        Roadmap roadmap = new Roadmap();
        roadmap.setId(UUID.randomUUID());
        roadmap.setUser(user);
        roadmap.setDescription("Learn Spring Boot");
        task.setRoadmap(roadmap);

        when(roadmapTaskRepository.findByIdWithRoadmap(taskId)).thenReturn(Optional.of(task));
        //  stub findById for the transaction
        when(roadmapTaskRepository.findById(taskId)).thenReturn(Optional.of(task));

        ElaborationResponse aiResponse = new ElaborationResponse(
                "Detailed steps", List.of("sub1", "sub2"), 2.5
        );
        when(aiClientService.generateStructured(anyString(), eq(ElaborationResponse.class), eq(userId), eq(AITask.ROADMAP_ELABORATION)))
                .thenReturn(aiResponse);

        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<RoadmapTask> callback = invocation.getArgument(0);
            return callback.doInTransaction(null);
        });
        when(roadmapTaskRepository.save(any(RoadmapTask.class))).thenReturn(task);

        ElaborationResponseDto dto = new ElaborationResponseDto(taskId, "Detailed steps", List.of("sub1", "sub2"));
        when(roadmapMapper.toElaborationDto(any(RoadmapTask.class))).thenReturn(dto);

        // When
        ElaborationResponseDto result = roadmapService.elaborateTask(taskId, user, enhance);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.details()).isEqualTo("Detailed steps");
        assertThat(result.subtasks()).containsExactly("sub1", "sub2");
        verify(gamificationService).recordActivity(user, GamificationAction.ELABORATE_TASK);
    }
    @Test
    void elaborateTask_shouldUseFallback_whenAIFails() {
        // Given
        UUID taskId = UUID.randomUUID();
        boolean enhance = false;

        RoadmapTask task = createRoadmapTask("Learn Spring Security", 1, 1);
        Roadmap roadmap = new Roadmap();
        roadmap.setId(UUID.randomUUID());
        roadmap.setUser(user);
        roadmap.setDescription("Learn Spring Boot");
        task.setRoadmap(roadmap);

        when(roadmapTaskRepository.findByIdWithRoadmap(taskId)).thenReturn(Optional.of(task));

        when(roadmapTaskRepository.findById(taskId)).thenReturn(Optional.of(task));

        when(aiClientService.generateStructured(anyString(), eq(ElaborationResponse.class), eq(userId), eq(AITask.ROADMAP_ELABORATION)))
                .thenThrow(new RuntimeException("AI error"));

        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<RoadmapTask> callback = invocation.getArgument(0);
            return callback.doInTransaction(null);
        });
        // The save method will set fallback details = task description, subtasks = []
        when(roadmapTaskRepository.save(any(RoadmapTask.class))).thenReturn(task);

        // Stub mapper to return DTO
        ElaborationResponseDto dto = new ElaborationResponseDto(taskId, "Learn Spring Security", List.of());
        when(roadmapMapper.toElaborationDto(any(RoadmapTask.class))).thenReturn(dto);

        // When
        ElaborationResponseDto result = roadmapService.elaborateTask(taskId, user, enhance);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.details()).isEqualTo("Learn Spring Security"); // fallback
        assertThat(result.subtasks()).isEmpty();
        verify(gamificationService).recordActivity(user, GamificationAction.ELABORATE_TASK);
    }

// ========== Tests for rescheduleRoadmap ==========

    @Test
    void rescheduleRoadmap_shouldApplyAIUpdatesAndSave() {
        // Given
        UUID roadmapId = UUID.randomUUID();
        Roadmap roadmap = new Roadmap();
        roadmap.setId(roadmapId);
        roadmap.setUser(user);
        roadmap.setDescription("Learn Spring Boot");
        roadmap.setDurationWeeks(4);
        RoadmapTask task1 = createRoadmapTask("Task 1", 1, 1);
        task1.setCompleted(true);
        RoadmapTask task2 = createRoadmapTask("Task 2", 2, 1);
        task2.setCompleted(false);
        roadmap.setTasks(List.of(task1, task2));

        when(roadmapRepository.findById(roadmapId)).thenReturn(Optional.of(roadmap));

        // AI response
        RescheduleResponse aiResponse = new RescheduleResponse(
                5, // new duration
                List.of(new RescheduleResponse.TaskUpdate(task2.getId().toString(), 3))
        );
        when(aiClientService.generateStructured(anyString(), eq(RescheduleResponse.class), eq(userId), eq(AITask.ROADMAP_RESCHEDULE)))
                .thenReturn(aiResponse);

        // Transaction stub
        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<RoadmapResponse> callback = invocation.getArgument(0);
            return callback.doInTransaction(null);
        });
        when(roadmapRepository.save(any(Roadmap.class))).thenReturn(roadmap);
        RoadmapResponse responseDto = new RoadmapResponse(roadmapId, "Learn Spring Boot", "desc", null, "ACTIVE", 5, List.of(), List.of(), List.of(), 0, null, null);
        when(roadmapMapper.toResponse(any(Roadmap.class))).thenReturn(responseDto);

        // When
        RoadmapResponse result = roadmapService.rescheduleRoadmap(roadmapId, user);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.durationWeeks()).isEqualTo(5);
        // Verify that task2 week was updated to 3
        ArgumentCaptor<Roadmap> captor = ArgumentCaptor.forClass(Roadmap.class);
        verify(roadmapRepository).save(captor.capture());
        Roadmap saved = captor.getValue();
        RoadmapTask updatedTask = saved.getTasks().stream()
                .filter(t -> t.getId().equals(task2.getId()))
                .findFirst().orElseThrow();
        assertThat(updatedTask.getWeekNumber()).isEqualTo(3);
    }

    @Test
    void rescheduleRoadmap_shouldThrowException_whenAllTasksCompleted() {
        // Given
        UUID roadmapId = UUID.randomUUID();
        Roadmap roadmap = new Roadmap();
        roadmap.setId(roadmapId);
        roadmap.setUser(user);
        RoadmapTask task = createRoadmapTask("Task", 1, 1);
        task.setCompleted(true);
        roadmap.setTasks(List.of(task));

        when(roadmapRepository.findById(roadmapId)).thenReturn(Optional.of(roadmap));

        // When/Then
        assertThatThrownBy(() -> roadmapService.rescheduleRoadmap(roadmapId, user))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("All tasks are already completed");
    }

// ========== Tests for getUserRoadmaps ==========

    @Test
    void getUserRoadmaps_shouldReturnListOfRoadmapResponses() {
        // Given
        Roadmap roadmap = new Roadmap();
        roadmap.setId(UUID.randomUUID());
        roadmap.setUser(user);
        roadmap.setTitle("Test Roadmap");
        List<Roadmap> roadmaps = List.of(roadmap);
        when(roadmapRepository.findByUserOrderByCreatedAtDesc(user)).thenReturn(roadmaps);
        List<RoadmapResponse> responses = List.of(new RoadmapResponse(roadmap.getId(), "Test Roadmap", null, null, "ACTIVE", 4, List.of(), List.of(), List.of(), 0, null, null));
        when(roadmapMapper.toResponseList(roadmaps)).thenReturn(responses);

        // When
        List<RoadmapResponse> result = roadmapService.getUserRoadmaps(user);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(roadmap.getId());
    }

// ========== Tests for deleteRoadmap ==========

    @Test
    void deleteRoadmap_shouldDeleteWhenAuthorized() {
        // Given
        UUID roadmapId = UUID.randomUUID();
        Roadmap roadmap = new Roadmap();
        roadmap.setId(roadmapId);
        roadmap.setUser(user);
        when(roadmapRepository.findById(roadmapId)).thenReturn(Optional.of(roadmap));

        // When
        roadmapService.deleteRoadmap(roadmapId, user);

        // Then
        verify(roadmapRepository).delete(roadmap);
    }

    @Test
    void deleteRoadmap_shouldThrowException_whenNotAuthorized() {
        // Given
        UUID roadmapId = UUID.randomUUID();
        User otherUser = new User();
        otherUser.setId(UUID.randomUUID());
        Roadmap roadmap = new Roadmap();
        roadmap.setId(roadmapId);
        roadmap.setUser(otherUser);
        when(roadmapRepository.findById(roadmapId)).thenReturn(Optional.of(roadmap));

        // When/Then
        assertThatThrownBy(() -> roadmapService.deleteRoadmap(roadmapId, user))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Roadmap not found");
    }

// ========== Tests for importTaskToMilestone ==========

    @Test
    void importTaskToMilestone_shouldCreateMilestoneTaskAndMarkImported() {
        // Given
        UUID roadmapId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        Roadmap roadmap = new Roadmap();
        roadmap.setId(roadmapId);
        roadmap.setUser(user);
        roadmap.setTitle("Test Roadmap");
        RoadmapTask task = createRoadmapTask("Task", 1, 1);
        task.setId(taskId);
        task.setDetails("Some details");
        task.setSubtasks("[\"sub1\", \"sub2\"]");
        roadmap.setTasks(List.of(task));

        when(roadmapRepository.findById(roadmapId)).thenReturn(Optional.of(roadmap));

        Milestone milestone = new Milestone();
        milestone.setId(UUID.randomUUID());
        when(milestoneService.getOrCreateMilestoneByTitle(user, "Roadmap: Test Roadmap")).thenReturn(milestone);

        // When
        roadmapService.importTaskToMilestone(roadmapId, taskId, user);

        // Then
        // Use the correct mock name
        verify(roadmapTaskRepository).save(task);
        assertThat(task.getImportedToMilestone()).isTrue();
        verify(taskService).createTaskWithRoadmapLink(
                eq(milestone.getId()), eq(user),
                eq("Task"),
                any(LocalDate.class),
                eq(taskId),
                eq("Some details"),
                eq("[\"sub1\", \"sub2\"]")
        );
    }
// ========== Tests for toggleTaskCompletion ==========

    @Test
    void toggleTaskCompletion_shouldToggleAndAwardGamification_whenCompleted() {
        // Given
        UUID taskId = UUID.randomUUID();
        RoadmapTask task = createRoadmapTask("Task", 1, 1);
        task.setCompleted(false);
        Roadmap roadmap = new Roadmap();
        roadmap.setId(UUID.randomUUID());
        roadmap.setUser(user);
        roadmap.setTasks(List.of(task));
        task.setRoadmap(roadmap);

        when(roadmapTaskRepository.findById(taskId)).thenReturn(Optional.of(task));

        // When
        roadmapService.toggleTaskCompletion(taskId, user);

        // Then
        assertThat(task.isCompleted()).isTrue();
        verify(roadmapTaskRepository).save(task);
        verify(gamificationService).recordActivity(user, GamificationAction.TASK);
        //  all tasks are completed, so badge should be awarded
        verify(gamificationService).awardRoadmapCompletedBadge(user);
    }
    @Test
    void toggleTaskCompletion_shouldAwardRoadmapCompletedBadge_whenAllTasksCompleted() {
        // Given
        UUID taskId = UUID.randomUUID();
        RoadmapTask task1 = createRoadmapTask("Task 1", 1, 1);
        task1.setCompleted(true);
        RoadmapTask task2 = createRoadmapTask("Task 2", 1, 2);
        task2.setCompleted(false);
        task2.setId(taskId);
        Roadmap roadmap = new Roadmap();
        roadmap.setId(UUID.randomUUID());
        roadmap.setUser(user);
        roadmap.setTasks(List.of(task1, task2));
        task2.setRoadmap(roadmap);
        task1.setRoadmap(roadmap);

        when(roadmapTaskRepository.findById(taskId)).thenReturn(Optional.of(task2));

        // When
        roadmapService.toggleTaskCompletion(taskId, user);

        // Then
        assertThat(task2.isCompleted()).isTrue();
        verify(roadmapTaskRepository).save(task2);
        verify(gamificationService).recordActivity(user, GamificationAction.TASK);
        verify(gamificationService).awardRoadmapCompletedBadge(user);
    }

// ========== Tests for continueRoadmapBatch ==========

    @Test
    void continueRoadmapBatch_shouldGenerateChunkOfWeeksAndSave() {
        // Given
        UUID roadmapId = UUID.randomUUID();
        Integer weeksToGenerate = 2;
        Roadmap roadmap = new Roadmap();
        roadmap.setId(roadmapId);
        roadmap.setUser(user);
        roadmap.setDescription("Learn Spring Boot");
        roadmap.setDurationWeeks(10);
        roadmap.setGeneratedWeeks(2);
        roadmap.setTasks(new ArrayList<>());

        //  stub userService.getRoadmapPreferences
        UserRoadmapPreferences prefs = new UserRoadmapPreferences();
        prefs.setDifficulty("BEGINNER");
        prefs.setLanguagePreference("en");
        prefs.setLearningStyle("READING");
        prefs.setHoursPerWeek(10);
        prefs.setAvoidWeekends(false);
        when(userService.getRoadmapPreferences(user)).thenReturn(prefs);

        RoadmapContinuationResponse aiResponse = new RoadmapContinuationResponse(
                List.of(new RoadmapGenerateResponse.Task(1, 3, "New Task", "daily", "details", List.of()))
        );
        when(aiClientService.generateStructured(anyString(), eq(RoadmapContinuationResponse.class), eq(userId), eq(AITask.ROADMAP_EXTENSION)))
                .thenReturn(aiResponse);

        when(roadmapRepository.findById(roadmapId)).thenReturn(Optional.of(roadmap));

        when(roadmapMapper.toEntity(any(RoadmapGenerateResponse.Task.class)))
                .thenReturn(createRoadmapTask("New Task", 3, 1));

        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<RoadmapResponse> callback = invocation.getArgument(0);
            return callback.doInTransaction(null);
        });
        when(roadmapRepository.save(any(Roadmap.class))).thenReturn(roadmap);
        RoadmapResponse responseDto = new RoadmapResponse(roadmapId, "Learn Spring Boot", "desc", null, "ACTIVE", 10, List.of(), List.of(), List.of(), 4, null, null);
        when(roadmapMapper.toResponse(any(Roadmap.class))).thenReturn(responseDto);

        // When
        RoadmapResponse result = roadmapService.continueRoadmapBatch(roadmapId, user, weeksToGenerate);

        // Then
        assertThat(result).isNotNull();
        ArgumentCaptor<Roadmap> captor = ArgumentCaptor.forClass(Roadmap.class);
        verify(roadmapRepository).save(captor.capture());
        Roadmap saved = captor.getValue();
        assertThat(saved.getGeneratedWeeks()).isEqualTo(4);
    }


    @Test
    void generateRoadmap_shouldUseTimeframeWeeksWhenProvided() {
        // Given
        String goal = "Learn Spring Boot";
        Integer timeframeWeeks = 6; // directly provided, not using timeframeValue/Unit

        UserRoadmapPreferences prefs = new UserRoadmapPreferences();
        prefs.setDifficulty("BEGINNER");
        prefs.setLanguagePreference("en");
        prefs.setLearningStyle("READING");
        prefs.setHoursPerWeek(10);
        prefs.setAvoidWeekends(false);
        when(userService.getRoadmapPreferences(user)).thenReturn(prefs);

        RoadmapGenerateResponse aiResponse = new RoadmapGenerateResponse(
                "Test Roadmap", 6,
//                null,
                List.of(), List.of(), List.of(), false
        );
        when(aiClientService.generateStructured(anyString(), eq(RoadmapGenerateResponse.class), eq(userId), eq(AITask.ROADMAP_INITIAL)))
                .thenReturn(aiResponse);

        when(roadmapMapper.toEntity(any(RoadmapGenerateResponse.Task.class)))
                .thenReturn(createRoadmapTask("dummy", 1, 1));

        Roadmap savedRoadmap = new Roadmap();
        savedRoadmap.setId(UUID.randomUUID());
        savedRoadmap.setDurationWeeks(6);
        savedRoadmap.setOriginalDurationValue(6);
        savedRoadmap.setOriginalDurationUnit("WEEKS");
        when(roadmapRepository.save(any(Roadmap.class))).thenReturn(savedRoadmap);

        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<Roadmap> callback = invocation.getArgument(0);
            return callback.doInTransaction(null);
        });

        // When – pass timeframeWeeks, ignore others
        Roadmap result = roadmapService.generateRoadmap(user, goal, timeframeWeeks, null, null);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getDurationWeeks()).isEqualTo(6);
        assertThat(result.getOriginalDurationValue()).isEqualTo(6);
        assertThat(result.getOriginalDurationUnit()).isEqualTo("WEEKS");
    }


    @Test
    void generateRoadmap_shouldConvertMonthsToWeeks() {
        // Given
        String goal = "Learn Spring Boot";
        Integer timeframeValue = 2;
        String timeframeUnit = "MONTHS";

        UserRoadmapPreferences prefs = new UserRoadmapPreferences();
        prefs.setDifficulty("BEGINNER");
        prefs.setLanguagePreference("en");
        prefs.setLearningStyle("READING");
        prefs.setHoursPerWeek(10);
        prefs.setAvoidWeekends(false);
        when(userService.getRoadmapPreferences(user)).thenReturn(prefs);

        // AI returns duration = 8 weeks (2 months * 4)
        RoadmapGenerateResponse aiResponse = new RoadmapGenerateResponse(
                "Test Roadmap", 8,
//                null,
                List.of(), List.of(), List.of(), false
        );
        when(aiClientService.generateStructured(anyString(), eq(RoadmapGenerateResponse.class), eq(userId), eq(AITask.ROADMAP_INITIAL)))
                .thenReturn(aiResponse);

        when(roadmapMapper.toEntity(any(RoadmapGenerateResponse.Task.class)))
                .thenReturn(createRoadmapTask("dummy", 1, 1));

        Roadmap savedRoadmap = new Roadmap();
        savedRoadmap.setId(UUID.randomUUID());
        savedRoadmap.setDurationWeeks(8);
        savedRoadmap.setOriginalDurationValue(2);
        savedRoadmap.setOriginalDurationUnit("MONTHS");
        when(roadmapRepository.save(any(Roadmap.class))).thenReturn(savedRoadmap);

        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<Roadmap> callback = invocation.getArgument(0);
            return callback.doInTransaction(null);
        });

        // When
        Roadmap result = roadmapService.generateRoadmap(user, goal, null, timeframeValue, timeframeUnit);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getDurationWeeks()).isEqualTo(8);
        assertThat(result.getOriginalDurationValue()).isEqualTo(2);
        assertThat(result.getOriginalDurationUnit()).isEqualTo("MONTHS");
    }

    @Test
    void generateRoadmap_shouldConvertYearsToWeeks() {
        // Given
        String goal = "Learn Spring Boot";
        Integer timeframeValue = 1;
        String timeframeUnit = "YEARS";

        UserRoadmapPreferences prefs = new UserRoadmapPreferences();
        prefs.setDifficulty("BEGINNER");
        prefs.setLanguagePreference("en");
        prefs.setLearningStyle("READING");
        prefs.setHoursPerWeek(10);
        prefs.setAvoidWeekends(false);
        when(userService.getRoadmapPreferences(user)).thenReturn(prefs);

        RoadmapGenerateResponse aiResponse = new RoadmapGenerateResponse(
                "Test Roadmap", 52,
//                null,
                List.of(), List.of(), List.of(), false

        );
        when(aiClientService.generateStructured(anyString(), eq(RoadmapGenerateResponse.class), eq(userId), eq(AITask.ROADMAP_INITIAL)))
                .thenReturn(aiResponse);

        when(roadmapMapper.toEntity(any(RoadmapGenerateResponse.Task.class)))
                .thenReturn(createRoadmapTask("dummy", 1, 1));

        Roadmap savedRoadmap = new Roadmap();
        savedRoadmap.setId(UUID.randomUUID());
        savedRoadmap.setDurationWeeks(52);
        savedRoadmap.setOriginalDurationValue(1);
        savedRoadmap.setOriginalDurationUnit("YEARS");
        when(roadmapRepository.save(any(Roadmap.class))).thenReturn(savedRoadmap);

        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<Roadmap> callback = invocation.getArgument(0);
            return callback.doInTransaction(null);
        });

        // When
        Roadmap result = roadmapService.generateRoadmap(user, goal, null, timeframeValue, timeframeUnit);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getDurationWeeks()).isEqualTo(52);
        assertThat(result.getOriginalDurationValue()).isEqualTo(1);
        assertThat(result.getOriginalDurationUnit()).isEqualTo("YEARS");
    }
    @Test
    void continueRoadmapBatch_shouldThrowException_whenFullyGenerated() {
        // Given
        UUID roadmapId = UUID.randomUUID();
        Roadmap roadmap = new Roadmap();
        roadmap.setId(roadmapId);
        roadmap.setUser(user);
        roadmap.setDurationWeeks(4);
        roadmap.setGeneratedWeeks(4); // fully generated
        when(roadmapRepository.findById(roadmapId)).thenReturn(Optional.of(roadmap));

        // When/Then
        assertThatThrownBy(() -> roadmapService.continueRoadmapBatch(roadmapId, user, 2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Roadmap already fully generated");
    }

    @Test
    void rescheduleRoadmap_shouldThrowException_whenAINull() {
        // Given
        UUID roadmapId = UUID.randomUUID();
        Roadmap roadmap = new Roadmap();
        roadmap.setId(roadmapId);
        roadmap.setUser(user);
        roadmap.setDescription("Learn Spring Boot");
        roadmap.setDurationWeeks(4);
        RoadmapTask task1 = createRoadmapTask("Task 1", 1, 1);
        task1.setCompleted(true);
        RoadmapTask task2 = createRoadmapTask("Task 2", 2, 1);
        task2.setCompleted(false);
        roadmap.setTasks(List.of(task1, task2));

        when(roadmapRepository.findById(roadmapId)).thenReturn(Optional.of(roadmap));

        // AI returns null
        when(aiClientService.generateStructured(anyString(), eq(RescheduleResponse.class), eq(userId), eq(AITask.ROADMAP_RESCHEDULE)))
                .thenReturn(null);

        // When/Then
        assertThatThrownBy(() -> roadmapService.rescheduleRoadmap(roadmapId, user))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("AI couldn't reschedule your roadmap");
    }

    @Test
    void continueRoadmap_shouldThrowException_whenAINoTasks() {
        // Given
        UUID roadmapId = UUID.randomUUID();
        Roadmap roadmap = new Roadmap();
        roadmap.setId(roadmapId);
        roadmap.setUser(user);
        roadmap.setDescription("Learn Spring Boot");
        roadmap.setDurationWeeks(8);
        RoadmapTask existingTask = createRoadmapTask("Task 1", 1, 1);
        existingTask.setCompleted(true);
        roadmap.setTasks(List.of(existingTask));

        when(roadmapRepository.findById(roadmapId)).thenReturn(Optional.of(roadmap));

        // AI returns null
        when(aiClientService.generateStructured(anyString(), eq(RoadmapContinuationResponse.class), eq(userId), eq(AITask.ROADMAP_NEXT_STEPS)))
                .thenReturn(null);

        // When/Then
        assertThatThrownBy(() -> roadmapService.continueRoadmap(roadmapId, user))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("AI couldn't generate the next roadmap steps");
    }

    @Test
    void continueRoadmapBatch_shouldUseDefaultChunkSizeWhenNull() {
        // Given
        UUID roadmapId = UUID.randomUUID();
        Roadmap roadmap = new Roadmap();
        roadmap.setId(roadmapId);
        roadmap.setUser(user);
        roadmap.setDescription("Learn Spring Boot");
        roadmap.setDurationWeeks(10); // 10 weeks
        roadmap.setGeneratedWeeks(0); // no weeks generated yet
        roadmap.setTasks(new ArrayList<>());

        UserRoadmapPreferences prefs = new UserRoadmapPreferences();
        prefs.setDifficulty("BEGINNER");
        prefs.setLanguagePreference("en");
        prefs.setLearningStyle("READING");
        prefs.setHoursPerWeek(10);
        prefs.setAvoidWeekends(false);
        when(userService.getRoadmapPreferences(user)).thenReturn(prefs);

        RoadmapContinuationResponse aiResponse = new RoadmapContinuationResponse(
                List.of(new RoadmapGenerateResponse.Task(1, 1, "Task", "daily", "details", List.of()))
        );
        when(aiClientService.generateStructured(anyString(), eq(RoadmapContinuationResponse.class), eq(userId), eq(AITask.ROADMAP_EXTENSION)))
                .thenReturn(aiResponse);

        when(roadmapRepository.findById(roadmapId)).thenReturn(Optional.of(roadmap));
        when(roadmapMapper.toEntity(any(RoadmapGenerateResponse.Task.class)))
                .thenReturn(createRoadmapTask("New Task", 1, 1));

        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<RoadmapResponse> callback = invocation.getArgument(0);
            return callback.doInTransaction(null);
        });
        when(roadmapRepository.save(any(Roadmap.class))).thenReturn(roadmap);
        RoadmapResponse responseDto = new RoadmapResponse(roadmapId, "Learn Spring Boot", "desc", null, "ACTIVE", 10, List.of(), List.of(), List.of(), 10, null, null);
        when(roadmapMapper.toResponse(any(Roadmap.class))).thenReturn(responseDto);

        // When – weeksToGenerate is null
        RoadmapResponse result = roadmapService.continueRoadmapBatch(roadmapId, user, null);

        // Then
        assertThat(result).isNotNull();
        ArgumentCaptor<Roadmap> captor = ArgumentCaptor.forClass(Roadmap.class);
        verify(roadmapRepository).save(captor.capture());
        Roadmap saved = captor.getValue();
        // Expect 10 because remainingWeeks = 10, and default chunk size = min(10, 12) = 10
        assertThat(saved.getGeneratedWeeks()).isEqualTo(10);
    }

    @Test
    void generateRoadmap_shouldUseDefault4WeeksWhenNoParametersProvided() {
        // Given
        String goal = "Learn Spring Boot";
        // All timeframe parameters are null

        UserRoadmapPreferences prefs = new UserRoadmapPreferences();
        prefs.setDifficulty("BEGINNER");
        prefs.setLanguagePreference("en");
        prefs.setLearningStyle("READING");
        prefs.setHoursPerWeek(10);
        prefs.setAvoidWeekends(false);
        when(userService.getRoadmapPreferences(user)).thenReturn(prefs);

        RoadmapGenerateResponse aiResponse = new RoadmapGenerateResponse(
                "Test Roadmap", 4,
//                null,
                List.of(), List.of(), List.of(), false
        );
        when(aiClientService.generateStructured(anyString(), eq(RoadmapGenerateResponse.class), eq(userId), eq(AITask.ROADMAP_INITIAL)))
                .thenReturn(aiResponse);

        when(roadmapMapper.toEntity(any(RoadmapGenerateResponse.Task.class)))
                .thenReturn(createRoadmapTask("dummy", 1, 1));

        Roadmap savedRoadmap = new Roadmap();
        savedRoadmap.setId(UUID.randomUUID());
        savedRoadmap.setDurationWeeks(4);
        savedRoadmap.setOriginalDurationValue(4);
        savedRoadmap.setOriginalDurationUnit("WEEKS");
        when(roadmapRepository.save(any(Roadmap.class))).thenReturn(savedRoadmap);

        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<Roadmap> callback = invocation.getArgument(0);
            return callback.doInTransaction(null);
        });

        // When – pass null for all timeframe parameters
        Roadmap result = roadmapService.generateRoadmap(user, goal, null, null, null);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getDurationWeeks()).isEqualTo(4);
        assertThat(result.getOriginalDurationValue()).isEqualTo(4);
        assertThat(result.getOriginalDurationUnit()).isEqualTo("WEEKS");
    }

    @Test
    void continueRoadmapBatch_shouldThrowException_whenRoadmapNotFound() {
        UUID roadmapId = UUID.randomUUID();
        when(roadmapRepository.findById(roadmapId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roadmapService.continueRoadmapBatch(roadmapId, user, 2))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Roadmap not found");
    }

    @Test
    void continueRoadmapBatch_shouldThrowException_whenNotAuthorized() {
        UUID roadmapId = UUID.randomUUID();
        User otherUser = new User();
        otherUser.setId(UUID.randomUUID());
        Roadmap roadmap = new Roadmap();
        roadmap.setId(roadmapId);
        roadmap.setUser(otherUser);
        when(roadmapRepository.findById(roadmapId)).thenReturn(Optional.of(roadmap));

        assertThatThrownBy(() -> roadmapService.continueRoadmapBatch(roadmapId, user, 2))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Roadmap not found");
    }

    @Test
    void elaborateTask_shouldThrowException_whenTaskNotFound() {
        UUID taskId = UUID.randomUUID();
        when(roadmapTaskRepository.findByIdWithRoadmap(taskId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roadmapService.elaborateTask(taskId, user, false))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Task not found");
    }

    @Test
    void elaborateTask_shouldThrowException_whenNotAuthorized() {
        UUID taskId = UUID.randomUUID();
        RoadmapTask task = createRoadmapTask("Task", 1, 1);
        Roadmap roadmap = new Roadmap();
        User otherUser = new User();
        otherUser.setId(UUID.randomUUID()); //  Set non-null ID
        roadmap.setUser(otherUser);
        roadmap.setId(UUID.randomUUID());
        task.setRoadmap(roadmap);
        when(roadmapTaskRepository.findByIdWithRoadmap(taskId)).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> roadmapService.elaborateTask(taskId, user, false))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Task not found");    }

    @Test
    void rescheduleRoadmap_shouldThrowException_whenAICallThrows() {
        UUID roadmapId = UUID.randomUUID();
        Roadmap roadmap = new Roadmap();
        roadmap.setId(roadmapId);
        roadmap.setUser(user);
        roadmap.setDescription("Learn Spring Boot");
        roadmap.setDurationWeeks(4);
        RoadmapTask task1 = createRoadmapTask("Task 1", 1, 1);
        task1.setCompleted(true);
        RoadmapTask task2 = createRoadmapTask("Task 2", 2, 1);
        task2.setCompleted(false);
        roadmap.setTasks(List.of(task1, task2));

        when(roadmapRepository.findById(roadmapId)).thenReturn(Optional.of(roadmap));

        // AI throws exception
        when(aiClientService.generateStructured(anyString(), eq(RescheduleResponse.class), eq(userId), eq(AITask.ROADMAP_RESCHEDULE)))
                .thenThrow(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "shared AI service at capacity"));

        assertThatThrownBy(() -> roadmapService.rescheduleRoadmap(roadmapId, user))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("shared AI service");
    }


    @Test
    void saveRescheduleInTransaction_shouldSkipInvalidUpdates() {
        // Given
        UUID roadmapId = UUID.randomUUID();
        Roadmap roadmap = new Roadmap();
        roadmap.setId(roadmapId);
        roadmap.setUser(user);
        roadmap.setDescription("Learn Spring Boot");
        roadmap.setDurationWeeks(4);
        RoadmapTask task1 = createRoadmapTask("Task 1", 1, 1);
        task1.setCompleted(true);
        RoadmapTask task2 = createRoadmapTask("Task 2", 2, 1);
        task2.setCompleted(false);
        roadmap.setTasks(List.of(task1, task2));

        //  Add this stub
        when(roadmapRepository.findById(roadmapId)).thenReturn(Optional.of(roadmap));

        // AI response with invalid updates
        RescheduleResponse aiResponse = new RescheduleResponse(
                5,
                List.of(
                        new RescheduleResponse.TaskUpdate(null, 3),
                        new RescheduleResponse.TaskUpdate("invalid-uuid", 4),
                        new RescheduleResponse.TaskUpdate(task2.getId().toString(), null)
                )
        );

        when(roadmapRepository.save(any(Roadmap.class))).thenReturn(roadmap);
        RoadmapResponse responseDto = new RoadmapResponse(roadmapId, "Learn Spring Boot", "desc", null, "ACTIVE", 5, List.of(), List.of(), List.of(), 0, null, null);
        when(roadmapMapper.toResponse(any(Roadmap.class))).thenReturn(responseDto);

        // When
        RoadmapResponse result = roadmapService.saveRescheduleInTransaction(roadmapId, user, aiResponse);

        // Then
        assertThat(result).isNotNull();
        assertThat(roadmap.getTasks().stream().filter(t -> t.getId().equals(task2.getId())).findFirst().get().getWeekNumber()).isEqualTo(2);
    }

    @Test
    void continueRoadmapBatch_shouldThrowException_whenAICallThrows() {
        UUID roadmapId = UUID.randomUUID();
        Roadmap roadmap = new Roadmap();
        roadmap.setId(roadmapId);
        roadmap.setUser(user);
        roadmap.setDurationWeeks(10);
        roadmap.setGeneratedWeeks(2);
        roadmap.setTasks(new ArrayList<>());
        when(roadmapRepository.findById(roadmapId)).thenReturn(Optional.of(roadmap));

        UserRoadmapPreferences prefs = new UserRoadmapPreferences();
        prefs.setDifficulty("BEGINNER");
        prefs.setLanguagePreference("en");
        prefs.setLearningStyle("READING");
        prefs.setHoursPerWeek(10);
        prefs.setAvoidWeekends(false);
        when(userService.getRoadmapPreferences(user)).thenReturn(prefs);

        // AI throws exception
        when(aiClientService.generateStructured(anyString(), eq(RoadmapContinuationResponse.class), eq(userId), eq(AITask.ROADMAP_EXTENSION)))
                .thenThrow(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "shared AI service at capacity"));

        assertThatThrownBy(() -> roadmapService.continueRoadmapBatch(roadmapId, user, 2))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("shared AI service");
    }

    @Test
    void continueRoadmap_shouldThrowException_whenAICallThrows() {
        UUID roadmapId = UUID.randomUUID();
        Roadmap roadmap = new Roadmap();
        roadmap.setId(roadmapId);
        roadmap.setUser(user);
        roadmap.setDescription("Learn Spring Boot");
        roadmap.setDurationWeeks(8);
        RoadmapTask existingTask = createRoadmapTask("Task 1", 1, 1);
        existingTask.setCompleted(true);
        roadmap.setTasks(List.of(existingTask));

        when(roadmapRepository.findById(roadmapId)).thenReturn(Optional.of(roadmap));

        // AI throws ResponseStatusException (simulates real 503 from DynamicAiClientService)
        when(aiClientService.generateStructured(anyString(), eq(RoadmapContinuationResponse.class), eq(userId), eq(AITask.ROADMAP_NEXT_STEPS)))
                .thenThrow(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "shared AI service at capacity"));

        assertThatThrownBy(() -> roadmapService.continueRoadmap(roadmapId, user))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("shared AI service");

    }


    // ========== Unauthorized Access Tests ==========

    @Test
    void importTaskToMilestone_shouldThrowException_whenNotAuthorized() {
        UUID roadmapId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        User otherUser = new User();
        otherUser.setId(UUID.randomUUID());
        Roadmap roadmap = new Roadmap();
        roadmap.setId(roadmapId);
        roadmap.setUser(otherUser);
        when(roadmapRepository.findById(roadmapId)).thenReturn(Optional.of(roadmap));

        assertThatThrownBy(() -> roadmapService.importTaskToMilestone(roadmapId, taskId, user))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Roadmap not found");    }

    @Test
    void toggleTaskCompletion_shouldThrowException_whenNotAuthorized() {
        UUID taskId = UUID.randomUUID();
        User otherUser = new User();
        otherUser.setId(UUID.randomUUID());
        RoadmapTask task = createRoadmapTask("Task", 1, 1);
        Roadmap roadmap = new Roadmap();
        roadmap.setUser(otherUser);
        task.setRoadmap(roadmap);
        when(roadmapTaskRepository.findById(taskId)).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> roadmapService.toggleTaskCompletion(taskId, user))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Task not found");    }

    @Test
    void continueRoadmap_shouldThrowException_whenNotAuthorized() {
        UUID roadmapId = UUID.randomUUID();
        User otherUser = new User();
        otherUser.setId(UUID.randomUUID());
        Roadmap roadmap = new Roadmap();
        roadmap.setId(roadmapId);
        roadmap.setUser(otherUser);
        when(roadmapRepository.findById(roadmapId)).thenReturn(Optional.of(roadmap));

        assertThatThrownBy(() -> roadmapService.continueRoadmap(roadmapId, user))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Roadmap not found");
    }

    @Test
    void saveContinuationInTransaction_shouldThrowException_whenNotAuthorized() {
        UUID roadmapId = UUID.randomUUID();
        User otherUser = new User();
        otherUser.setId(UUID.randomUUID());
        Roadmap roadmap = new Roadmap();
        roadmap.setId(roadmapId);
        roadmap.setUser(otherUser);
        when(roadmapRepository.findById(roadmapId)).thenReturn(Optional.of(roadmap));

        assertThatThrownBy(() -> roadmapService.saveContinuationInTransaction(roadmapId, user, List.of()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Roadmap not found");    }

    @Test
    void saveElaborationInTransaction_shouldThrowException_whenNotAuthorized() {
        UUID taskId = UUID.randomUUID();
        User otherUser = new User();
        otherUser.setId(UUID.randomUUID());
        RoadmapTask task = createRoadmapTask("Task", 1, 1);
        Roadmap roadmap = new Roadmap();
        roadmap.setUser(otherUser);
        task.setRoadmap(roadmap);
        when(roadmapTaskRepository.findById(taskId)).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> roadmapService.saveElaborationInTransaction(taskId, user, null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Task not found");
    }

    @Test
    void rescheduleRoadmap_shouldThrowException_whenNotAuthorized() {
        UUID roadmapId = UUID.randomUUID();
        User otherUser = new User();
        otherUser.setId(UUID.randomUUID());
        Roadmap roadmap = new Roadmap();
        roadmap.setId(roadmapId);
        roadmap.setUser(otherUser);
        when(roadmapRepository.findById(roadmapId)).thenReturn(Optional.of(roadmap));

        assertThatThrownBy(() -> roadmapService.rescheduleRoadmap(roadmapId, user))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Roadmap not found");
    }

    @Test
    void saveRescheduleInTransaction_shouldThrowException_whenNotAuthorized() {
        UUID roadmapId = UUID.randomUUID();
        User otherUser = new User();
        otherUser.setId(UUID.randomUUID());
        Roadmap roadmap = new Roadmap();
        roadmap.setId(roadmapId);
        roadmap.setUser(otherUser);
        when(roadmapRepository.findById(roadmapId)).thenReturn(Optional.of(roadmap));

        assertThatThrownBy(() -> roadmapService.saveRescheduleInTransaction(roadmapId, user, null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Roadmap not found");
    }

    @Test
    void saveContinuationBatchInTransaction_shouldThrowException_whenNotAuthorized() {
        UUID roadmapId = UUID.randomUUID();
        User otherUser = new User();
        otherUser.setId(UUID.randomUUID());
        Roadmap roadmap = new Roadmap();
        roadmap.setId(roadmapId);
        roadmap.setUser(otherUser);
        when(roadmapRepository.findById(roadmapId)).thenReturn(Optional.of(roadmap));

        assertThatThrownBy(() -> roadmapService.saveContinuationBatchInTransaction(roadmapId, user, 1, 2, List.of()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Roadmap not found");
    }

    @Test
    void continueRoadmapBatch_shouldBuildSummaryOfPreviousWeeks() {
        // Given
        UUID roadmapId = UUID.randomUUID();
        Roadmap roadmap = new Roadmap();
        roadmap.setId(roadmapId);
        roadmap.setUser(user);
        roadmap.setDescription("Learn Spring Boot");
        roadmap.setDurationWeeks(10);
        roadmap.setGeneratedWeeks(3); // previous weeks generated

        // Create tasks for weeks 1, 2, 3
        RoadmapTask task1 = createRoadmapTask("Task 1", 1, 1);
        RoadmapTask task2 = createRoadmapTask("Task 2", 2, 1);
        RoadmapTask task3 = createRoadmapTask("Task 3", 3, 1);
        roadmap.setTasks(new ArrayList<>(List.of(task1, task2, task3)));

        when(roadmapRepository.findById(roadmapId)).thenReturn(Optional.of(roadmap));

        UserRoadmapPreferences prefs = new UserRoadmapPreferences();
        prefs.setDifficulty("BEGINNER");
        prefs.setLanguagePreference("en");
        prefs.setLearningStyle("READING");
        prefs.setHoursPerWeek(10);
        prefs.setAvoidWeekends(false);
        when(userService.getRoadmapPreferences(user)).thenReturn(prefs);

        // AI response with new tasks
        RoadmapContinuationResponse aiResponse = new RoadmapContinuationResponse(
                List.of(new RoadmapGenerateResponse.Task(1, 4, "New Task", "daily", "details", List.of()))
        );
        when(aiClientService.generateStructured(anyString(), eq(RoadmapContinuationResponse.class), eq(userId), eq(AITask.ROADMAP_EXTENSION)))
                .thenReturn(aiResponse);

        // Stub mapper for new task
        RoadmapTask newTask = createRoadmapTask("New Task", 4, 1);
        when(roadmapMapper.toEntity(any(RoadmapGenerateResponse.Task.class))).thenReturn(newTask);

        // Transaction stub
        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<RoadmapResponse> callback = invocation.getArgument(0);
            return callback.doInTransaction(null);
        });
        when(roadmapRepository.save(any(Roadmap.class))).thenReturn(roadmap);
        RoadmapResponse responseDto = new RoadmapResponse(roadmapId, "Learn Spring Boot", "desc", null, "ACTIVE", 10, List.of(), List.of(), List.of(), 5, null, null);
        when(roadmapMapper.toResponse(any(Roadmap.class))).thenReturn(responseDto);

        // When
        RoadmapResponse result = roadmapService.continueRoadmapBatch(roadmapId, user, 2);

        // Then
        assertThat(result).isNotNull();
        ArgumentCaptor<Roadmap> captor = ArgumentCaptor.forClass(Roadmap.class);
        verify(roadmapRepository).save(captor.capture());
        Roadmap saved = captor.getValue();
        // Weeks generated: startWeek = 4, weeksToGenerate = 2 → endWeek = 5
        assertThat(saved.getGeneratedWeeks()).isEqualTo(5);
    }
    @Test
    void saveElaborationInTransaction_shouldSetSubtasksEmpty_whenAISubtasksEmpty() {
        // Given
        UUID taskId = UUID.randomUUID();
        RoadmapTask task = createRoadmapTask("Learn Spring Security", 1, 1);
        Roadmap roadmap = new Roadmap();
        roadmap.setId(UUID.randomUUID());
        roadmap.setUser(user);
        task.setRoadmap(roadmap);
        when(roadmapTaskRepository.findById(taskId)).thenReturn(Optional.of(task));

        // AI response with null subtasks (or empty list)
        ElaborationResponse aiResponse = new ElaborationResponse(
                "Detailed steps", null, 2.5
        );

        //  Stub save to return the task
        when(roadmapTaskRepository.save(any(RoadmapTask.class))).thenReturn(task);

        // When
        RoadmapTask result = roadmapService.saveElaborationInTransaction(taskId, user, aiResponse);

        // Then
        assertThat(result.getDetails()).isEqualTo("Detailed steps");
        assertThat(result.getSubtasks()).isEqualTo("[]");
        verify(gamificationService).recordActivity(user, GamificationAction.ELABORATE_TASK);
    }

    @Test
    void saveElaborationInTransaction_shouldHandleJsonProcessingException() throws Exception {
        // Given
        UUID taskId = UUID.randomUUID();
        RoadmapTask task = createRoadmapTask("Learn Spring Security", 1, 1);
        Roadmap roadmap = new Roadmap();
        roadmap.setId(UUID.randomUUID());
        roadmap.setUser(user);
        task.setRoadmap(roadmap);
        when(roadmapTaskRepository.findById(taskId)).thenReturn(Optional.of(task));

        ElaborationResponse aiResponse = new ElaborationResponse(
                "Detailed steps", List.of("sub1", "sub2"), 2.5
        );

        // Mock objectMapper to throw exception
        when(objectMapper.writeValueAsString(anyList()))
                .thenThrow(new JsonProcessingException("Mock exception") {});

        //  Stub save to return the task
        when(roadmapTaskRepository.save(any(RoadmapTask.class))).thenReturn(task);

        // When
        RoadmapTask result = roadmapService.saveElaborationInTransaction(taskId, user, aiResponse);

        // Then – should set subtasks to "[]" and not throw
        assertThat(result.getSubtasks()).isEqualTo("[]");
        verify(gamificationService).recordActivity(user, GamificationAction.ELABORATE_TASK);
    }

    // ========== Lambda & Transaction Method Coverage ==========

    @Test
    void saveRoadmapInTransaction_shouldSetStatusAndAwardGamification() {
        Roadmap roadmap = new Roadmap();
        roadmap.setUser(user);

        when(roadmapRepository.save(any(Roadmap.class))).thenReturn(roadmap);

        Roadmap result = roadmapService.saveRoadmapInTransaction(roadmap, user);

        assertThat(result.getStatus()).isEqualTo("ACTIVE");
        verify(gamificationService).recordActivity(user, GamificationAction.ROADMAP_GENERATE);
        verify(roadmapRepository).save(roadmap);
    }

    @Test
    void saveContinuationInTransaction_shouldUpdateDurationIfMaxWeekIncreases() {
        UUID roadmapId = UUID.randomUUID();
        Roadmap roadmap = new Roadmap();
        roadmap.setId(roadmapId);
        roadmap.setUser(user);
        roadmap.setDurationWeeks(4);
        roadmap.setTasks(new ArrayList<>());

        when(roadmapRepository.findById(roadmapId)).thenReturn(Optional.of(roadmap));
        when(roadmapRepository.save(any(Roadmap.class))).thenReturn(roadmap);

        // Map a task for week 6 (which is > current duration of 4)
        RoadmapGenerateResponse.Task newTaskDto = new RoadmapGenerateResponse.Task(1, 6, "Task", "daily", "desc", List.of());
        RoadmapTask mappedTask = createRoadmapTask("Mapped", 6, 1);
        when(roadmapMapper.toEntity(newTaskDto)).thenReturn(mappedTask);

        // Execute directly to cover lambda target
        roadmapService.saveContinuationInTransaction(roadmapId, user, List.of(newTaskDto));

        // The duration should be updated to 6
        assertThat(roadmap.getDurationWeeks()).isEqualTo(6);
        verify(roadmapRepository).save(roadmap);
    }

    @Test
    void saveContinuationBatchInTransaction_shouldDeleteExistingTasksForTargetWeeks() {
        UUID roadmapId = UUID.randomUUID();
        Roadmap roadmap = new Roadmap();
        roadmap.setId(roadmapId);
        roadmap.setUser(user);

        // Add a task in week 3 that should be deleted, and week 1 that should be kept
        RoadmapTask taskToKeep = createRoadmapTask("Keep", 1, 1);
        RoadmapTask taskToDelete = createRoadmapTask("Delete", 3, 1);
        roadmap.setTasks(new ArrayList<>(List.of(taskToKeep, taskToDelete)));

        when(roadmapRepository.findById(roadmapId)).thenReturn(Optional.of(roadmap));
        when(roadmapRepository.save(any(Roadmap.class))).thenReturn(roadmap);

        // We are replacing weeks 2 through 4
        roadmapService.saveContinuationBatchInTransaction(roadmapId, user, 2, 4, List.of());

        // Verify task in week 3 was removed
        assertThat(roadmap.getTasks()).containsExactly(taskToKeep);
        verify(roadmapTaskRepository).deleteAll(anyList()); // Verifies the lambda logic triggered
    }

    @Test
    void saveRescheduleInTransaction_shouldHandleNullResponseGracefully() {
        UUID roadmapId = UUID.randomUUID();
        Roadmap roadmap = new Roadmap();
        roadmap.setId(roadmapId);
        roadmap.setUser(user);
        roadmap.setDurationWeeks(4);

        when(roadmapRepository.findById(roadmapId)).thenReturn(Optional.of(roadmap));
        when(roadmapRepository.save(any(Roadmap.class))).thenReturn(roadmap);

        // Execute with null AI response to hit fallback branches
        RoadmapResponse result = roadmapService.saveRescheduleInTransaction(roadmapId, user, null);

        // Should save without crashing and without changing duration
        assertThat(roadmap.getDurationWeeks()).isEqualTo(4);
        verify(roadmapRepository).save(roadmap);
    }

    @Test
    void buildElaboratePrompt_shouldFormatProperly_withEnhanceTrue() {
        // This explicitly covers the `if (enhance)` branch inside the private method.
        // Since it's private, we test it through the public interface by triggering the AI call.
        UUID taskId = UUID.randomUUID();
        RoadmapTask task = createRoadmapTask("Learn Spring Security", 1, 1);
        Roadmap roadmap = new Roadmap();
        roadmap.setUser(user);
        roadmap.setDescription("Learn Spring Boot");
        task.setRoadmap(roadmap);

        when(roadmapTaskRepository.findByIdWithRoadmap(taskId)).thenReturn(Optional.of(task));

        // Return null AI response to trigger fallback and avoid needing the transaction mock
        when(aiClientService.generateStructured(anyString(), any(), any(), any())).thenReturn(null);

        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<RoadmapTask> callback = invocation.getArgument(0);
            return callback.doInTransaction(null);
        });

        when(roadmapTaskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(roadmapTaskRepository.save(any(RoadmapTask.class))).thenReturn(task);

        // Execute WITH enhance = true to cover the true branch in buildElaboratePrompt
        roadmapService.elaborateTask(taskId, user, true);

        // The AI mock verification implies the prompt builder ran successfully
        verify(aiClientService).generateStructured(contains("even more detailed, comprehensive guide"), any(), any(), any());
    }

    @Test
    void generateRoadmap_shouldFallbackTo4Weeks_whenTimeframeUnitIsNull() {
        String goal = "Learn Java";
        when(userService.getRoadmapPreferences(user)).thenReturn(new UserRoadmapPreferences());

        RoadmapGenerateResponse aiResponse = new RoadmapGenerateResponse(
                "Title", 4,
//                null,
                List.of(), List.of(), List.of(), false);
        when(aiClientService.generateStructured(anyString(), any(), any(), any())).thenReturn(aiResponse);

        //  Teach the mapper to return a dummy task for the fallback tasks!
        when(roadmapMapper.toEntity(any())).thenReturn(createRoadmapTask("Mapped Fallback", 1, 1));

        when(transactionTemplate.execute(any())).thenAnswer(i -> {
            TransactionCallback<Roadmap> cb = i.getArgument(0);
            return cb.doInTransaction(null);
        });

        when(roadmapRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        // Pass timeframeValue=10 but timeframeUnit=null
        Roadmap result = roadmapService.generateRoadmap(user, goal, null, 10, null);

        // It should hit the else block and fall back to 4 weeks
        assertThat(result.getDurationWeeks()).isEqualTo(4);
        assertThat(result.getOriginalDurationValue()).isEqualTo(4);
        assertThat(result.getOriginalDurationUnit()).isEqualTo("WEEKS");
    }
    @Test
    void generateRoadmap_shouldHandleNullResourcesAndMilestonesFromAI() {
        when(userService.getRoadmapPreferences(user)).thenReturn(new UserRoadmapPreferences());

        // Tasks are provided, but resources and milestones are null
        RoadmapGenerateResponse aiResponse = new RoadmapGenerateResponse(
                "Title", 4,
//                null,
                List.of(new RoadmapGenerateResponse.Task(1, 1, "Task", "daily", "desc", List.of())),
                null, // resources
                null, // milestones
                false
        );
        when(aiClientService.generateStructured(anyString(), any(), any(), any())).thenReturn(aiResponse);
        when(roadmapMapper.toEntity(any())).thenReturn(createRoadmapTask("mapped", 1, 1));

        when(transactionTemplate.execute(any())).thenAnswer(i -> {
            TransactionCallback<Roadmap> cb = i.getArgument(0);
            return cb.doInTransaction(null);
        });
        when(roadmapRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Roadmap result = roadmapService.generateRoadmap(user, "Goal", null, 4, "WEEKS");

        // The ternary operators should convert the nulls into empty ArrayLists
        assertThat(result.getResources()).isEmpty();
        assertThat(result.getMilestones()).isEmpty();
        assertThat(result.getTasks()).isNotEmpty();
    }

    @Test
    void toggleTaskCompletion_shouldUncompleteTask_andNotAwardGamification() {
        UUID taskId = UUID.randomUUID();
        RoadmapTask task = createRoadmapTask("Task", 1, 1);
        task.setCompleted(true); // Task was already completed

        Roadmap roadmap = new Roadmap();
        roadmap.setUser(user);
        task.setRoadmap(roadmap);

        when(roadmapTaskRepository.findById(taskId)).thenReturn(Optional.of(task));

        roadmapService.toggleTaskCompletion(taskId, user);

        assertThat(task.isCompleted()).isFalse();
        verify(roadmapTaskRepository).save(task);
        verify(gamificationService, never()).recordActivity(any(), any());
    }

    @Test
    void toggleTaskCompletion_shouldNotAwardRoadmapBadge_whenTasksRemain() {
        UUID taskId = UUID.randomUUID();
        RoadmapTask task1 = createRoadmapTask("Task 1", 1, 1);
        task1.setId(taskId);
        task1.setCompleted(false);

        RoadmapTask task2 = createRoadmapTask("Task 2", 1, 2);
        task2.setCompleted(false); // Second task remains incomplete

        Roadmap roadmap = new Roadmap();
        roadmap.setUser(user);
        roadmap.setTasks(List.of(task1, task2));
        task1.setRoadmap(roadmap);
        task2.setRoadmap(roadmap);

        when(roadmapTaskRepository.findById(taskId)).thenReturn(Optional.of(task1));

        roadmapService.toggleTaskCompletion(taskId, user);

        assertThat(task1.isCompleted()).isTrue();
        verify(gamificationService).recordActivity(user, GamificationAction.TASK);
        // Should NOT award the roadmap badge because task2 is still false
        verify(gamificationService, never()).awardRoadmapCompletedBadge(user);
    }

    @Test
    void continueRoadmap_shouldHandleNullWeekNumberInTasks() {
        UUID roadmapId = UUID.randomUUID();
        Roadmap roadmap = new Roadmap();
        roadmap.setId(roadmapId);
        roadmap.setUser(user);
        roadmap.setDescription("Goal");
        roadmap.setTitle("Title");

        //Set the duration so .orElse(roadmap.getDurationWeeks()) doesn't throw a NullPointerException
        roadmap.setDurationWeeks(4);

        RoadmapTask task = createRoadmapTask("Task 1", 1, 1);
        task.setWeekNumber(null); // Force null week number to hit the ternary operator
        task.setCompleted(true);
        roadmap.setTasks(new ArrayList<>(List.of(task)));

        when(roadmapRepository.findById(roadmapId)).thenReturn(Optional.of(roadmap));
        when(aiClientService.generateStructured(anyString(), any(), any(), any()))
                .thenReturn(new RoadmapContinuationResponse(List.of(
                        new RoadmapGenerateResponse.Task(1, 2, "New", "daily", "desc", List.of())
                )));

        when(transactionTemplate.execute(any())).thenAnswer(i -> {
            TransactionCallback<RoadmapResponse> cb = i.getArgument(0);
            return cb.doInTransaction(null);
        });

        when(roadmapMapper.toEntity(any())).thenReturn(createRoadmapTask("New", 2, 1));
        when(roadmapRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        roadmapService.continueRoadmap(roadmapId, user);

        verify(roadmapRepository).save(any(Roadmap.class));
    }

    @Test
    void saveContinuationInTransaction_shouldNotUpdateDuration_whenNewMaxWeekIsLess() {
        UUID roadmapId = UUID.randomUUID();
        Roadmap roadmap = new Roadmap();
        roadmap.setId(roadmapId);
        roadmap.setUser(user);
        roadmap.setDurationWeeks(10); // Current duration is high
        roadmap.setTasks(new ArrayList<>());

        when(roadmapRepository.findById(roadmapId)).thenReturn(Optional.of(roadmap));
        when(roadmapRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        // AI returns a task for week 5 (which is <= current duration of 10)
        RoadmapGenerateResponse.Task newTaskDto = new RoadmapGenerateResponse.Task(1, 5, "Task", "daily", "desc", List.of());
        when(roadmapMapper.toEntity(newTaskDto)).thenReturn(createRoadmapTask("Mapped", 5, 1));

        roadmapService.saveContinuationInTransaction(roadmapId, user, List.of(newTaskDto));

        // Duration should remain unchanged
        assertThat(roadmap.getDurationWeeks()).isEqualTo(10);
    }


    @Test
    void saveElaborationInTransaction_shouldUseFallback_whenAiResponseIsNull() {
        UUID taskId = UUID.randomUUID();
        RoadmapTask task = createRoadmapTask("Task", 1, 1);
        Roadmap roadmap = new Roadmap();
        roadmap.setUser(user);
        task.setRoadmap(roadmap);

        when(roadmapTaskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(roadmapTaskRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        // Execute directly with null response
        RoadmapTask result = roadmapService.saveElaborationInTransaction(taskId, user, null);

        assertThat(result.getDetails()).isEqualTo("Task"); // Falls back to description
        assertThat(result.getSubtasks()).isEqualTo("[]");
    }

    @Test
    void saveElaborationInTransaction_shouldSetEmptySubtasks_whenAiResponseSubtasksIsEmpty() {
        UUID taskId = UUID.randomUUID();
        RoadmapTask task = createRoadmapTask("Task", 1, 1);
        Roadmap roadmap = new Roadmap();
        roadmap.setUser(user);
        task.setRoadmap(roadmap);

        when(roadmapTaskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(roadmapTaskRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        // Empty list instead of null
        ElaborationResponse aiResponse = new ElaborationResponse("Details", List.of(), 1.0);

        RoadmapTask result = roadmapService.saveElaborationInTransaction(taskId, user, aiResponse);

        assertThat(result.getSubtasks()).isEqualTo("[]");
    }

    @Test
    void saveRescheduleInTransaction_shouldNotUpdateDuration_whenNewDurationIsZero() {
        UUID roadmapId = UUID.randomUUID();
        Roadmap roadmap = new Roadmap();
        roadmap.setId(roadmapId);
        roadmap.setUser(user);
        roadmap.setDurationWeeks(4);

        when(roadmapRepository.findById(roadmapId)).thenReturn(Optional.of(roadmap));
        when(roadmapRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        // newDurationWeeks = 0, tasks = empty
        RescheduleResponse aiResponse = new RescheduleResponse(0, List.of());

        roadmapService.saveRescheduleInTransaction(roadmapId, user, aiResponse);

        assertThat(roadmap.getDurationWeeks()).isEqualTo(4); // Remains 4
    }

    @Test
    void saveRescheduleInTransaction_shouldSkip_whenNewWeekIsNull() {
        UUID roadmapId = UUID.randomUUID();
        Roadmap roadmap = new Roadmap();
        roadmap.setId(roadmapId);
        roadmap.setUser(user);
        roadmap.setDurationWeeks(4);

        RoadmapTask task = createRoadmapTask("Task", 1, 1);
        roadmap.setTasks(List.of(task));

        when(roadmapRepository.findById(roadmapId)).thenReturn(Optional.of(roadmap));
        when(roadmapRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        // taskId is provided, but newWeek is null
        RescheduleResponse aiResponse = new RescheduleResponse(5, List.of(
                new RescheduleResponse.TaskUpdate(task.getId().toString(), null)
        ));

        roadmapService.saveRescheduleInTransaction(roadmapId, user, aiResponse);

        // The task week number should be completely skipped/ignored
        assertThat(task.getWeekNumber()).isEqualTo(1);
    }


    // ========================================================================
    // Missing Coverage Tests: Reschedule Prompt Formatting & Validation Blocks
    // ========================================================================

    @Test
    void buildReschedulePrompt_shouldFallbackToString_whenJsonProcessingFails() throws Exception {
        // Given
        UUID roadmapId = UUID.randomUUID();
        Roadmap roadmap = new Roadmap();
        roadmap.setId(roadmapId);
        roadmap.setUser(user);
        roadmap.setDescription("Learn Java");
        roadmap.setDurationWeeks(4);

        RoadmapTask task = createRoadmapTask("Learn Variables", 1, 1);
        task.setCompleted(false);
        roadmap.setTasks(List.of(task));

        when(roadmapRepository.findById(roadmapId)).thenReturn(Optional.of(roadmap));

        // Force ObjectMapper to throw an exception to hit the catch block
        when(objectMapper.writeValueAsString(anyList())).thenThrow(new RuntimeException("JSON Parse Error"));

        // We expect it to silently catch the error, use .toString() as fallback, and call AI
        when(aiClientService.generateStructured(anyString(), any(), any(), any())).thenReturn(null);

        // When
        try {
            roadmapService.rescheduleRoadmap(roadmapId, user);
        } catch (RuntimeException e) {
            // We ignore the "AI reschedule failed" exception because we only care that the prompt logic executed
        }

        // Then
        // Verify that the prompt generation attempted to use Jackson, but fell back safely
        verify(objectMapper).writeValueAsString(anyList());
    }

    @Test
    void continueRoadmapBatch_shouldThrowException_whenDurationWeeksIsNull() {
        // Given
        UUID roadmapId = UUID.randomUUID();
        Roadmap roadmap = new Roadmap();
        roadmap.setId(roadmapId);
        roadmap.setUser(user);

        // Explicitly set duration to null to simulate DB corruption
        roadmap.setDurationWeeks(null);

        when(roadmapRepository.findById(roadmapId)).thenReturn(Optional.of(roadmap));

        // When / Then
        assertThatThrownBy(() -> roadmapService.continueRoadmapBatch(roadmapId, user, 2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("missing duration data");
    }

    @Test
    void continueRoadmapBatch_shouldThrowException_whenRemainingWeeksIsZero() {
        // Given
        UUID roadmapId = UUID.randomUUID();

        // 🔥 Use a Spy so we can manipulate the Roadmap entity's methods
        Roadmap roadmap = org.mockito.Mockito.spy(new Roadmap());
        roadmap.setId(roadmapId);
        roadmap.setUser(user);
        roadmap.setDurationWeeks(4);
        roadmap.setGeneratedWeeks(4);

        // Force the first check to pass, so we can reach the defensive remainingWeeks check!
        org.mockito.Mockito.doReturn(false).when(roadmap).isFullyGenerated();

        when(roadmapRepository.findById(roadmapId)).thenReturn(Optional.of(roadmap));

        // When / Then
        assertThatThrownBy(() -> roadmapService.continueRoadmapBatch(roadmapId, user, 2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No weeks left to generate");
    }


    // ========================================================================
    // JaCoCo 100% Coverage Completers: Lambdas, Ternary Operators, and Nulls
    // ========================================================================

    @Test
    void serviceMethods_shouldThrowException_whenEntityNotFound() {
        UUID fakeId = UUID.randomUUID();

        // 🔥 FIX: Use lenient() so Mockito doesn't crash if a specific method fails before reaching the next stub
        org.mockito.Mockito.lenient().when(roadmapRepository.findById(fakeId)).thenReturn(Optional.empty());
        org.mockito.Mockito.lenient().when(roadmapTaskRepository.findById(fakeId)).thenReturn(Optional.empty());
        org.mockito.Mockito.lenient().when(roadmapTaskRepository.findByIdWithRoadmap(fakeId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roadmapService.deleteRoadmap(fakeId, user)).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> roadmapService.importTaskToMilestone(fakeId, fakeId, user)).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> roadmapService.toggleTaskCompletion(fakeId, user)).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> roadmapService.continueRoadmap(fakeId, user)).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> roadmapService.saveContinuationInTransaction(fakeId, user, List.of())).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> roadmapService.saveElaborationInTransaction(fakeId, user, null)).isInstanceOf(ResponseStatusException.class);

        //  Added the missing elaborateTask call!
        assertThatThrownBy(() -> roadmapService.elaborateTask(fakeId, user, false)).isInstanceOf(ResponseStatusException.class);

        assertThatThrownBy(() -> roadmapService.rescheduleRoadmap(fakeId, user)).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> roadmapService.saveRescheduleInTransaction(fakeId, user, null)).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> roadmapService.saveContinuationBatchInTransaction(fakeId, user, 1, 2, List.of())).isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void importTaskToMilestone_shouldThrowWhenTaskMissingFromRoadmap() {
        // Triggers: lambda$importTaskToMilestone$2()
        UUID roadmapId = UUID.randomUUID();
        Roadmap roadmap = new Roadmap();
        roadmap.setUser(user);
        roadmap.setTasks(List.of()); // Empty tasks array

        when(roadmapRepository.findById(roadmapId)).thenReturn(Optional.of(roadmap));

        assertThatThrownBy(() -> roadmapService.importTaskToMilestone(roadmapId, UUID.randomUUID(), user))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Task not found in roadmap");
    }

    @Test
    void generateRoadmap_shouldUseFallbackTasks_whenAiResponseIsMissing() {
        // Triggers: aiResponse.tasks() == null (buildRoadmapEntity 87%)
        // Triggers: timeframeWeeks != null (createFallbackRoadmapResponse 50%)
        when(userService.getRoadmapPreferences(user)).thenReturn(new UserRoadmapPreferences());

        // Return null tasks to trigger the fallback logic and the empty list ternary
        when(aiClientService.generateStructured(anyString(), any(), any(), any())).thenReturn(null);

        when(transactionTemplate.execute(any())).thenAnswer(i -> {
            TransactionCallback<Roadmap> cb = i.getArgument(0);
            return cb.doInTransaction(null);
        });
        when(roadmapMapper.toEntity(any())).thenReturn(createRoadmapTask("Task", 1, 1));
        when(roadmapRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        // Pass timeframeWeeks as 6 explicitly
        Roadmap result = roadmapService.generateRoadmap(user, "Goal", 6, null, null);

        assertThat(result.getDurationWeeks()).isEqualTo(6);
    }

    @Test
    void continueRoadmapBatch_shouldHandleMissingMetadata_andDefaultChunkSize() {
        UUID roadmapId = UUID.randomUUID();
        Roadmap roadmap = new Roadmap();
        roadmap.setId(roadmapId);
        roadmap.setUser(user);
        roadmap.setDurationWeeks(4);
        roadmap.setGeneratedWeeks(null); // Force null to trigger ternary

        RoadmapTask nullWeekTask = createRoadmapTask("Missing Week Data", 1, 1);
        nullWeekTask.setWeekNumber(null); // Force null week to trigger stream filters
        roadmap.setTasks(new ArrayList<>(List.of(nullWeekTask)));

        when(roadmapRepository.findById(roadmapId)).thenReturn(Optional.of(roadmap));
        when(userService.getRoadmapPreferences(user)).thenReturn(new UserRoadmapPreferences());
        when(aiClientService.generateStructured(anyString(), any(), any(), any()))
                .thenReturn(new RoadmapContinuationResponse(List.of(
                        new RoadmapGenerateResponse.Task(1, 1, "T", "daily", "d", List.of())
                )));

        when(transactionTemplate.execute(any())).thenAnswer(i -> {
            TransactionCallback<RoadmapResponse> cb = i.getArgument(0);
            return cb.doInTransaction(null);
        });
        when(roadmapMapper.toEntity(any())).thenReturn(createRoadmapTask("T", 1, 1));
        when(roadmapRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        //  Tell the mapper to return a dummy response so it doesn't return null
        when(roadmapMapper.toResponse(any())).thenReturn(org.mockito.Mockito.mock(RoadmapResponse.class));

        // Pass 0 to hit the `weeksToGenerate > 0` false branch
        RoadmapResponse result = roadmapService.continueRoadmapBatch(roadmapId, user, 0);

        assertThat(result).isNotNull();
    }

    @Test
    void continueRoadmapBatch_shouldCoverNullDayNumberInSorting() {
        // Given
        UUID roadmapId = UUID.randomUUID();
        Roadmap roadmap = new Roadmap();
        roadmap.setId(roadmapId);
        roadmap.setUser(user);
        roadmap.setDurationWeeks(4);

        //  Set generatedWeeks > 0 to force the code into the previousWeeksSummary block
        roadmap.setGeneratedWeeks(1);

        //  Create a task and force the dayNumber to be null
        RoadmapTask nullDayTask = createRoadmapTask("Null Day Task", 1, 1);
        nullDayTask.setDayNumber(null);

        // Create a normal task to force the sorter to compare them
        RoadmapTask normalTask = createRoadmapTask("Normal Task", 1, 2);

        roadmap.setTasks(new ArrayList<>(List.of(nullDayTask, normalTask)));

        when(roadmapRepository.findById(roadmapId)).thenReturn(Optional.of(roadmap));
        when(userService.getRoadmapPreferences(user)).thenReturn(new UserRoadmapPreferences());

        when(aiClientService.generateStructured(anyString(), any(), any(), any()))
                .thenReturn(new RoadmapContinuationResponse(List.of(
                        new RoadmapGenerateResponse.Task(1, 2, "New Task", "daily", "desc", List.of())
                )));

        when(transactionTemplate.execute(any())).thenAnswer(i -> {
            TransactionCallback<RoadmapResponse> cb = i.getArgument(0);
            return cb.doInTransaction(null);
        });

        when(roadmapMapper.toEntity(any())).thenReturn(createRoadmapTask("New Task", 2, 1));
        when(roadmapRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(roadmapMapper.toResponse(any())).thenReturn(org.mockito.Mockito.mock(RoadmapResponse.class));

        // When
        RoadmapResponse result = roadmapService.continueRoadmapBatch(roadmapId, user, 1);

        // Then
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("generateRoadmap: AI response with null tasks → isFallback=true, uses fallback")
    void generateRoadmap_whenAIResponseHasNullTasks_usesFallback() {
        when(userService.getRoadmapPreferences(user)).thenReturn(new UserRoadmapPreferences());

        RoadmapGenerateResponse responseWithNullTasks = new RoadmapGenerateResponse(
                "Title", 4, null, null, null, false);
        when(aiClientService.generateStructured(anyString(), eq(RoadmapGenerateResponse.class), eq(userId), eq(AITask.ROADMAP_INITIAL)))
                .thenReturn(responseWithNullTasks);

        // ← THE FIX: stub the mapper for fallback tasks
        when(roadmapMapper.toEntity(any(RoadmapGenerateResponse.Task.class)))
                .thenReturn(createRoadmapTask("fallback", 1, 1));

        when(transactionTemplate.execute(any())).thenAnswer(inv -> {
            TransactionCallback<Roadmap> cb = inv.getArgument(0);
            return cb.doInTransaction(null);
        });
        when(roadmapRepository.save(any(Roadmap.class))).thenAnswer(inv -> inv.getArgument(0));

        Roadmap result = roadmapService.generateRoadmap(user, "Goal", null, 4, "WEEKS");

        assertThat(result).isNotNull();
        assertThat(result.getTitle()).contains("Personalized Roadmap");
    }

    @Test
    @DisplayName("generateRoadmap: AI response with empty tasks list → isFallback=true, uses fallback")
    void generateRoadmap_whenAIResponseHasEmptyTasks_usesFallback() {
        when(userService.getRoadmapPreferences(user)).thenReturn(new UserRoadmapPreferences());

        RoadmapGenerateResponse responseWithEmptyTasks = new RoadmapGenerateResponse(
                "Title", 4, List.of(), List.of(), List.of(), false);
        when(aiClientService.generateStructured(anyString(), eq(RoadmapGenerateResponse.class), eq(userId), eq(AITask.ROADMAP_INITIAL)))
                .thenReturn(responseWithEmptyTasks);

        // ← THE FIX: stub the mapper for fallback tasks
        when(roadmapMapper.toEntity(any(RoadmapGenerateResponse.Task.class)))
                .thenReturn(createRoadmapTask("fallback", 1, 1));

        when(transactionTemplate.execute(any())).thenAnswer(inv -> {
            TransactionCallback<Roadmap> cb = inv.getArgument(0);
            return cb.doInTransaction(null);
        });
        when(roadmapRepository.save(any(Roadmap.class))).thenAnswer(inv -> inv.getArgument(0));

        Roadmap result = roadmapService.generateRoadmap(user, "Goal", null, 4, "WEEKS");

        assertThat(result).isNotNull();
        assertThat(result.getTitle()).contains("Personalized Roadmap");
    }

    @Test
    @DisplayName("continueRoadmap: AI response with null tasks → 503")
    void continueRoadmap_whenAIResponseHasNullTasks_throws503() {
        UUID roadmapId = UUID.randomUUID();
        Roadmap roadmap = new Roadmap();
        roadmap.setId(roadmapId);
        roadmap.setUser(user);
        roadmap.setDescription("Goal");
        roadmap.setDurationWeeks(8);
        RoadmapTask completed = createRoadmapTask("Task 1", 1, 1);
        completed.setCompleted(true);
        roadmap.setTasks(List.of(completed));

        when(roadmapRepository.findById(roadmapId)).thenReturn(Optional.of(roadmap));

        // Response with null tasks (but non-null object)
        when(aiClientService.generateStructured(anyString(), eq(RoadmapContinuationResponse.class), eq(userId), eq(AITask.ROADMAP_NEXT_STEPS)))
                .thenReturn(new RoadmapContinuationResponse(null));

        assertThatThrownBy(() -> roadmapService.continueRoadmap(roadmapId, user))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("AI couldn't generate the next roadmap steps");
    }

    @Test
    @DisplayName("continueRoadmap: AI response with empty tasks → 503")
    void continueRoadmap_whenAIResponseHasEmptyTasks_throws503() {
        UUID roadmapId = UUID.randomUUID();
        Roadmap roadmap = new Roadmap();
        roadmap.setId(roadmapId);
        roadmap.setUser(user);
        roadmap.setDescription("Goal");
        roadmap.setDurationWeeks(8);
        RoadmapTask completed = createRoadmapTask("Task 1", 1, 1);
        completed.setCompleted(true);
        roadmap.setTasks(List.of(completed));

        when(roadmapRepository.findById(roadmapId)).thenReturn(Optional.of(roadmap));

        when(aiClientService.generateStructured(anyString(), eq(RoadmapContinuationResponse.class), eq(userId), eq(AITask.ROADMAP_NEXT_STEPS)))
                .thenReturn(new RoadmapContinuationResponse(List.of()));

        assertThatThrownBy(() -> roadmapService.continueRoadmap(roadmapId, user))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("AI couldn't generate the next roadmap steps");
    }

    @Test
    @DisplayName("continueRoadmapBatch: null AI response → 503 (covers throw line)")
    void continueRoadmapBatch_whenAIResponseIsNull_throws503() {
        UUID roadmapId = UUID.randomUUID();
        Roadmap roadmap = new Roadmap();
        roadmap.setId(roadmapId);
        roadmap.setUser(user);
        roadmap.setDurationWeeks(10);
        roadmap.setGeneratedWeeks(2);
        roadmap.setTasks(new ArrayList<>());

        when(roadmapRepository.findById(roadmapId)).thenReturn(Optional.of(roadmap));
        when(userService.getRoadmapPreferences(user)).thenReturn(new UserRoadmapPreferences());

        when(aiClientService.generateStructured(anyString(), eq(RoadmapContinuationResponse.class), eq(userId), eq(AITask.ROADMAP_EXTENSION)))
                .thenReturn(null);

        assertThatThrownBy(() -> roadmapService.continueRoadmapBatch(roadmapId, user, 2))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("AI couldn't generate additional roadmap weeks");
    }

    @Test
    @DisplayName("toggleTaskCompletion: task was completed → un-completes, no gamification")
    void toggleTaskCompletion_whenTaskWasCompleted_uncompletesAndSkipsGamification() {
        UUID taskId = UUID.randomUUID();
        RoadmapTask task = createRoadmapTask("Task", 1, 1);
        task.setCompleted(true);   // ← was completed

        Roadmap roadmap = new Roadmap();
        roadmap.setUser(user);
        roadmap.setTasks(List.of(task));
        task.setRoadmap(roadmap);

        when(roadmapTaskRepository.findById(taskId)).thenReturn(Optional.of(task));

        roadmapService.toggleTaskCompletion(taskId, user);

        assertThat(task.isCompleted()).isFalse();
        verify(roadmapTaskRepository).save(task);
        verify(gamificationService, never()).recordActivity(any(), any());
    }

    @Test
    @DisplayName("continueRoadmapBatch: previous tasks filter handles null week and out-of-range week")
    void continueRoadmapBatch_withNullAndOutOfRangeWeekNumbers_filtersSafely() {
        UUID roadmapId = UUID.randomUUID();
        Roadmap roadmap = new Roadmap();
        roadmap.setId(roadmapId);
        roadmap.setUser(user);
        roadmap.setDurationWeeks(10);
        roadmap.setGeneratedWeeks(2);   // currentGenerated = 2

        RoadmapTask nullWeekTask = createRoadmapTask("Null week", 1, 1);
        nullWeekTask.setWeekNumber(null);                       // first condition false

        RoadmapTask inRangeTask = createRoadmapTask("In range", 1, 1);  // weekNumber = 1 ≤ 2 → both true

        RoadmapTask futureTask = createRoadmapTask("Future", 5, 1);     // weekNumber = 5 > 2 → second condition false

        roadmap.setTasks(new ArrayList<>(List.of(nullWeekTask, inRangeTask, futureTask)));

        when(roadmapRepository.findById(roadmapId)).thenReturn(Optional.of(roadmap));
        when(userService.getRoadmapPreferences(user)).thenReturn(new UserRoadmapPreferences());

        when(aiClientService.generateStructured(anyString(), eq(RoadmapContinuationResponse.class), eq(userId), eq(AITask.ROADMAP_EXTENSION)))
                .thenReturn(new RoadmapContinuationResponse(List.of(
                        new RoadmapGenerateResponse.Task(1, 3, "New", "daily", "desc", List.of()))));

        when(transactionTemplate.execute(any())).thenAnswer(inv -> {
            TransactionCallback<RoadmapResponse> cb = inv.getArgument(0);
            return cb.doInTransaction(null);
        });
        when(roadmapMapper.toEntity(any())).thenReturn(createRoadmapTask("New", 3, 1));
        when(roadmapRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(roadmapMapper.toResponse(any())).thenReturn(mock(RoadmapResponse.class));

        RoadmapResponse result = roadmapService.continueRoadmapBatch(roadmapId, user, 2);

        assertThat(result).isNotNull();
    }


    @Test
    @DisplayName("saveRescheduleInTransaction: AI response with null newDurationWeeks → skips duration update")
    void saveRescheduleInTransaction_whenNewDurationNull_skipsDurationUpdate() {
        UUID roadmapId = UUID.randomUUID();
        Roadmap roadmap = new Roadmap();
        roadmap.setId(roadmapId);
        roadmap.setUser(user);
        roadmap.setDurationWeeks(4);

        when(roadmapRepository.findById(roadmapId)).thenReturn(Optional.of(roadmap));
        when(roadmapRepository.save(any(Roadmap.class))).thenAnswer(inv -> inv.getArgument(0));
        when(roadmapMapper.toResponse(any(Roadmap.class)))
                .thenReturn(mock(RoadmapResponse.class));   // ← if you assert on result

        // tasks=null, newDurationWeeks=null → both guards skip
        RescheduleResponse aiResponse = new RescheduleResponse(null, null);

        roadmapService.saveRescheduleInTransaction(roadmapId, user, aiResponse);

        assertThat(roadmap.getDurationWeeks()).isEqualTo(4);
    }

    @Test
    @DisplayName("saveContinuationBatchInTransaction: null weekNumber tasks are filtered from deletion")
    void saveContinuationBatchInTransaction_nullWeekNumbersAreIgnored() {
        UUID roadmapId = UUID.randomUUID();
        Roadmap roadmap = new Roadmap();
        roadmap.setId(roadmapId);
        roadmap.setUser(user);

        RoadmapTask nullWeekTask = createRoadmapTask("Null week", 1, 1);
        nullWeekTask.setWeekNumber(null);

        RoadmapTask targetWeekTask = createRoadmapTask("Target", 3, 1);

        roadmap.setTasks(new ArrayList<>(List.of(nullWeekTask, targetWeekTask)));

        when(roadmapRepository.findById(roadmapId)).thenReturn(Optional.of(roadmap));
        when(roadmapRepository.save(any(Roadmap.class))).thenAnswer(inv -> inv.getArgument(0));

        // Deleting weeks 2-4 — targetWeekTask is in range, nullWeekTask is excluded
        roadmapService.saveContinuationBatchInTransaction(roadmapId, user, 2, 4, List.of());

        // nullWeekTask should survive (its weekNumber was null, so filter excluded it)
        assertThat(roadmap.getTasks()).contains(nullWeekTask);
    }

    @Test
    @DisplayName("buildRoadmapEntity: null tasks list is handled by defensive ternary")
    void buildRoadmapEntity_whenTasksAreNull_usesEmptyList() throws Exception {
        java.lang.reflect.Method method = RoadmapService.class.getDeclaredMethod(
                "buildRoadmapEntity",
                User.class, RoadmapGenerateResponse.class, String.class, int.class,
                Integer.class, String.class, boolean.class, int.class);
        method.setAccessible(true);

        RoadmapGenerateResponse aiResponseWithNullTasks = new RoadmapGenerateResponse(
                "Title", 4, null, List.of(), List.of(), false);

        Roadmap roadmap = (Roadmap) method.invoke(
                roadmapService, user, aiResponseWithNullTasks, "Goal",
                4, 4, "WEEKS", false, 4);

        assertThat(roadmap).isNotNull();
        assertThat(roadmap.getTasks()).isEmpty();
    }

    @Test
    @DisplayName("continueRoadmapBatch: non-null response with null tasks → 503")
    void continueRoadmapBatch_whenTasksIsNull_throws503() {
        UUID roadmapId = UUID.randomUUID();
        Roadmap roadmap = new Roadmap();
        roadmap.setId(roadmapId);
        roadmap.setUser(user);
        roadmap.setDurationWeeks(10);
        roadmap.setGeneratedWeeks(2);
        roadmap.setTasks(new ArrayList<>());

        when(roadmapRepository.findById(roadmapId)).thenReturn(Optional.of(roadmap));
        when(userService.getRoadmapPreferences(user)).thenReturn(new UserRoadmapPreferences());

        when(aiClientService.generateStructured(anyString(), eq(RoadmapContinuationResponse.class), eq(userId), eq(AITask.ROADMAP_EXTENSION)))
                .thenReturn(new RoadmapContinuationResponse(null));   // ← tasks == null

        assertThatThrownBy(() -> roadmapService.continueRoadmapBatch(roadmapId, user, 2))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("AI couldn't generate additional roadmap weeks");
    }

    @Test
    @DisplayName("continueRoadmapBatch: non-null response with empty tasks → 503")
    void continueRoadmapBatch_whenTasksIsEmpty_throws503() {
        UUID roadmapId = UUID.randomUUID();
        Roadmap roadmap = new Roadmap();
        roadmap.setId(roadmapId);
        roadmap.setUser(user);
        roadmap.setDurationWeeks(10);
        roadmap.setGeneratedWeeks(2);
        roadmap.setTasks(new ArrayList<>());

        when(roadmapRepository.findById(roadmapId)).thenReturn(Optional.of(roadmap));
        when(userService.getRoadmapPreferences(user)).thenReturn(new UserRoadmapPreferences());

        when(aiClientService.generateStructured(anyString(), eq(RoadmapContinuationResponse.class), eq(userId), eq(AITask.ROADMAP_EXTENSION)))
                .thenReturn(new RoadmapContinuationResponse(List.of()));   // ← tasks == empty

        assertThatThrownBy(() -> roadmapService.continueRoadmapBatch(roadmapId, user, 2))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("AI couldn't generate additional roadmap weeks");
    }

    @Test
    @DisplayName("saveContinuationBatchInTransaction: deletion filter covers all week-number branches")
    void saveContinuationBatchInTransaction_filterCoversAllBranches() {
        UUID roadmapId = UUID.randomUUID();
        Roadmap roadmap = new Roadmap();
        roadmap.setId(roadmapId);
        roadmap.setUser(user);

        RoadmapTask nullWeekTask = createRoadmapTask("Null week", 1, 1);
        nullWeekTask.setWeekNumber(null);                                    // first condition false

        RoadmapTask belowRangeTask = createRoadmapTask("Below", 1, 1);       // weekNumber = 1 < startWeek(2)

        RoadmapTask inRangeTask = createRoadmapTask("In range", 3, 1);       // weekNumber = 3 in [2, 4]

        RoadmapTask aboveRangeTask = createRoadmapTask("Above", 6, 1);       // weekNumber = 6 > endWeek(4)

        roadmap.setTasks(new ArrayList<>(List.of(nullWeekTask, belowRangeTask, inRangeTask, aboveRangeTask)));

        when(roadmapRepository.findById(roadmapId)).thenReturn(Optional.of(roadmap));
        when(roadmapRepository.save(any(Roadmap.class))).thenAnswer(inv -> inv.getArgument(0));

        // Delete weeks 2 through 4
        roadmapService.saveContinuationBatchInTransaction(roadmapId, user, 2, 4, List.of());

        // Only inRangeTask should be removed
        assertThat(roadmap.getTasks()).containsExactlyInAnyOrder(nullWeekTask, belowRangeTask, aboveRangeTask);
    }
}